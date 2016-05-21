/**
 * 
 */
package aho.util.mysql2PHP2Java;

import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

import aho.util.mysql2PHP2Java.exceptions.DatabaseWasNotFoundException;
import aho.util.mysql2PHP2Java.exceptions.InvalidPasswordException;
import aho.util.mysql2PHP2Java.exceptions.InvalidStateException;
import aho.util.mysql2PHP2Java.exceptions.NoCommandSpecifiedException;

/**
 * @author Adam Hlavacek
 *
 */
public class MySQL2PHP2Java {
    /*
     * Server can return this states:
     */
    public static final int STATE_OK = 0; // Everything was in normal
    public static final int STATE_DATABASE_NOT_FOUND = 1; // Database was not found
    public static final int STATE_NO_COMMAND_SPECIFIED = 2; // No command was specified
    public static final int STATE_WRONG_PASSWORD = 3; // Password used to connect to the script is invalid
    public static final int STATE_TEST_CON_PHP = 4; // Connection to the PHP script was successful
    public static final int STATE_TEST_CON_MySQL = 5; // Connection to the MySQL was successful
    public static final int STATE_INVALID = 255; // Returned state is not known

    private String url; // URL of PHP script
    private String password; // Password used to connect with

    private MySQLRespond last = null; // Respond from last SQL execution

    /**
     * Creates a new bridge on specified URL
     * 
     * @param url
     *            URL of PHP script on server
     */
    public MySQL2PHP2Java(String url) {
	this.url = url;
	this.password = "";
    }

    /**
     * Creates a new bridge on specified URL
     * 
     * @param url
     *            URL of PHP script on server
     * @param password
     *            Password used to connect to the PHP script
     */
    public MySQL2PHP2Java(String url, String password) {
	this.url = url;
	this.password = password;
    }

    /**
     * Executes SQL command to MySQL database by PHP script and saves it as last().
     * 
     * @param sqlcommand
     *            command to execute
     * @return MySQL respond for this command
     * @throws DatabaseWasNotFoundException
     * @throws InvalidPasswordException
     * @throws InvalidStateException
     * @throws NoCommandSpecifiedException
     * @throws IOException
     */
    public MySQLRespond executeSQL(String sqlcommand)
	    throws DatabaseWasNotFoundException, InvalidPasswordException, InvalidStateException, NoCommandSpecifiedException, IOException {
	String[] parameters = { "sql=" + sqlcommand + "" };
	String whereString = null; // Part of SQL command after word WHERE
	if (sqlcommand.toUpperCase().contains("WHERE")) {
	    int whereIdex = sqlcommand.toUpperCase().indexOf(" WHERE ");
	    whereString = sqlcommand.substring(whereIdex + 7);
	}
	return last = new MySQLRespond(executeScript(parameters), this, whereString);
    }

    /**
     * Returns respond from last SQL execution
     * 
     * @return respond from last SQL execution
     */
    public MySQLRespond last() {
	return last;
    }

    /**
     * Tests connection to PHP script.
     * 
     * @return true if server returned STATE_TEST_CON_PHP state, false otherwise.
     */
    public boolean testConn2PHPScript() {
	try {
	    final String[] parameters = { "testing=testPHP" };
	    switch (getReturnCode(executeScript(parameters))) {
	    case STATE_WRONG_PASSWORD:
		throw new InvalidPasswordException("PHP script refuges this password");
	    case STATE_TEST_CON_PHP:
		return true;
	    default:
		return false;
	    }
	} catch (Exception e) {
	    StringWriter sw = new StringWriter();
	    PrintWriter pw = new PrintWriter(sw);
	    e.printStackTrace(pw);
	    System.err.println(sw.toString());
	    return false;
	}
    }

    /**
     * Tests connection to MySQL.
     * 
     * @return true if server returned STATE_TEST_CON_MySQL state, false otherwise.
     */
    public boolean testConn2MySQL() {
	try {
	    final String[] parameters = { "testing=testMySQL" };
	    switch (getReturnCode(executeScript(parameters))) {
	    case STATE_WRONG_PASSWORD:
		throw new InvalidPasswordException("PHP script refuges this password");
	    case STATE_DATABASE_NOT_FOUND:
		throw new DatabaseWasNotFoundException("Database was not found");
	    case STATE_TEST_CON_MySQL:
		return true;
	    default:
		return false;
	    }
	} catch (Exception e) {
	    StringWriter sw = new StringWriter();
	    PrintWriter pw = new PrintWriter(sw);
	    e.printStackTrace(pw);
	    System.err.println(sw.toString());
	    return false;
	}
    }

    /**
     * Tests connection to PHP script and MySQL database
     * 
     * @return true if both works, false otherwise
     */
    public boolean testConn() {
	if (testConn2PHPScript())
	    return testConn2MySQL();
	return false;
    }

    /**
     * Sends request to the PHP script with given parameters. Password is not need to be given, it is added by
     * function itself.
     * 
     * @param PHPParameters
     *            Parameters used to send to the PHP script
     * @throws IOException
     */
    private String executeScript(final String[] PHPParameters) throws IOException {
	String parameters = "p=" + password + "&";
	for (String param : PHPParameters)
	    parameters += param + "&";

	StringBuilder response = new StringBuilder();

	byte[] postData = parameters.getBytes(StandardCharsets.UTF_8);
	int postDataLength = postData.length;

	URL obj = new URL(url);
	HttpURLConnection con = (HttpURLConnection) obj.openConnection();

	con.setDoOutput(true);
	con.setInstanceFollowRedirects(false);

	con.setRequestMethod("POST");

	con.setRequestProperty("Content-Type", "application/x-www-form-urlencoded");
	con.setRequestProperty("charset", "utf-8");
	con.setRequestProperty("Content-Length", Integer.toString(postDataLength));
	con.setUseCaches(false);

	DataOutputStream wr = new DataOutputStream(con.getOutputStream());
	wr.write(postData);

	BufferedReader in = null;
	try {
	    in = new BufferedReader(new InputStreamReader(con.getInputStream()));
	} catch (java.io.FileNotFoundException e) {
	    /*
	     * We need to change this, because default print is with parameters and that means it would print
	     * our password to the console
	     */
	    throw new java.io.FileNotFoundException("Script on adress " + this.url + " was not found");
	}
	String inputLine;

	while ((inputLine = in.readLine()) != null) {
	    response.append(inputLine);
	}

	return response.toString();
	// Response contains metadata about encoding, we should exclude them
	// return response.substring(response.indexOf("/>") + 2);
    }

    /**
     * Asks server for raw data, no formation is in place
     * 
     * @param sqlcommand
     *            SQL command to select wanted data
     * @param targetCollum
     *            collumn in which are raw data stored
     * @return byte array downloaded from respond of server
     * @throws IOException
     */
    public ByteArrayOutputStream getRawData(final String sqlcommand, final String targetCollum) throws IOException {
	final int buffSize = 1024; // How large is the buffered stream byte array

	String parameters = "p=" + password + "&getRaw=t&sql=" + URLEncoder.encode(sqlcommand, "UTF-8") + "&tcollumn=" + targetCollum;
	
	byte[] postData = parameters.getBytes(StandardCharsets.UTF_8);
	int postDataLength = postData.length;

	URL obj = new URL(url);
	byte[] buf = new byte[buffSize];

	HttpURLConnection connection = (HttpURLConnection)obj.openConnection();
	
	connection.setDoOutput(true);
	connection.setInstanceFollowRedirects(false);

	connection.setRequestMethod("POST");

	connection.setRequestProperty("Content-Type", "application/x-www-form-urlencoded");
	connection.setRequestProperty("charset", "utf-8");
	connection.setRequestProperty("Content-Length", Integer.toString(postDataLength));
	connection.setUseCaches(false);

	DataOutputStream wr = new DataOutputStream(connection.getOutputStream());
	wr.write(postData);
	
	InputStream inS = connection.getInputStream();

	int currLength = 0;

	ByteArrayOutputStream outStream = new ByteArrayOutputStream();

	while ((currLength = inS.read(buf)) != -1) {
	    outStream.write(buf, 0, currLength);
	}

	return outStream;
    }

    /**
     * Parses respond code from PHP script's respond
     * 
     * @param scriptRespond
     *            respond from PHP script
     * @return number of known parsed state, 255 otherwise
     */
    public static int getReturnCode(final String scriptRespond) {
	final char respondNumber = scriptRespond.charAt(0);

	if (Character.isDigit(respondNumber))
	    return Integer.parseInt("" + respondNumber);
	else
	    return STATE_INVALID;
    }

}
