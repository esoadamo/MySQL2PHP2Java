package aho.util.mysql2PHP2Java;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import aho.util.mysql2PHP2Java.exceptions.ColumnDoesNotExistException;
import aho.util.mysql2PHP2Java.exceptions.DatabaseWasNotFoundException;
import aho.util.mysql2PHP2Java.exceptions.InvalidPasswordException;
import aho.util.mysql2PHP2Java.exceptions.InvalidStateException;
import aho.util.mysql2PHP2Java.exceptions.NoCommandSpecifiedException;

public class MySQLRespond {
    private final List<String> columns = new ArrayList<String>();
    private final List<String[]> values = new ArrayList<String[]>();
    private final List<ByteArrayOutputStream> rawData = new ArrayList<ByteArrayOutputStream>();

    private int maxIndex = 0; // How many datas we have

    private int currIndex = -1; // Which index are we now using

    /**
     * Parses respond data from MySQL2PHP2Java and put them into columns and values.
     * 
     * @param respond
     *            text generated from PHP script
     * @param calledBy
     *            by which MySQL2PHP2Java instance was this program called
     * @param whereString
     *            That part of SQL statement after word WHERE (e. g. sql = "SELECT * from users WHERE id = 4"
     *            -> whereString = "id = 4")
     * @throws DatabaseWasNotFoundException
     * @throws InvalidPasswordException
     * @throws InvalidStateException
     * @throws NoCommandSpecifiedException
     * @throws IOException
     */
    public MySQLRespond(String respond, final MySQL2PHP2Java calledBy, final String whereString)
	    throws DatabaseWasNotFoundException, InvalidPasswordException, InvalidStateException, NoCommandSpecifiedException, IOException {
	int respondCode = MySQL2PHP2Java.getReturnCode(respond);
	switch (respondCode) {
	case MySQL2PHP2Java.STATE_OK:
	    break;
	case MySQL2PHP2Java.STATE_WRONG_PASSWORD:
	    throw new InvalidPasswordException("PHP script refuges this password");
	case MySQL2PHP2Java.STATE_DATABASE_NOT_FOUND:
	    throw new DatabaseWasNotFoundException("Database was not found");
	case MySQL2PHP2Java.STATE_NO_COMMAND_SPECIFIED:
	    throw new NoCommandSpecifiedException("You have not specified any command");
	default:
	    throw new InvalidStateException("Invalid respond code: " + respondCode);
	}

	/*
	 * Every line can be splitted by <BR>, but wee need to remove first line, because it tells us
	 * only respond code, no data.
	 */
	final String[] respondLines = respond.substring(respond.indexOf("<BR>") + 4).split("<BR>");

	maxIndex = respondLines.length - 1;

	/*
	 * In case we have no data to parse from, set max index to none and return
	 */
	if ((respondLines.length <= 1) && (respondLines[0].length() == 0)) {
	    maxIndex = -1;
	    return;
	}

	for (int i = 0; i < respondLines.length; i++) {
	    /*
	     * Example respond:
	     * Array ( ], [id] => [1], [username] => [Míša)
	     * Array ( ], [id] => [2 ], [username] => [David )
	     */

	    /*
	     * Remove "Array ( ], [" from beginning and ")" from end -> [id] => [1], [username] => [Míša
	     * "[" at beginning and "]" at the end of line are not wanted
	     */
	    String line = respondLines[i].substring(13, respondLines[i].length() - 1);

	    /*
	     * Split every column and value into their array, so:
	     * 0 = id] => [1
	     * 1 = username] => [Míša
	     */
	    String[] columnsAndValues = line.split("], \\[");
	    for (int x = 0; x < columnsAndValues.length; x++) {
		/*
		 * Split it to column and value only, so:
		 * 0: username
		 * 1: Míša
		 */
		String[] parts = columnsAndValues[x].split("] => \\[");

		/*
		 * If calledBy is not null -> we expect blob (raw data) somewhere.
		 * So, if value (part[1]) is not null and it starts with "blob" it means it
		 * is a blob and we have to download as raw data. A String link is placed (@rawData:X, where X
		 * is index of raw data in rawData list)
		 */
		if ((calledBy != null) && (parts.length > 1) && (parts[1].startsWith("blob"))) {
		    /*
		     * Example of part[1] if it's a blob: blob::table::column
		     * split part[1] with "::" ->
		     * [0] = blob Indication that this data is a blob
		     * [1] = table (Table with this blob)
		     * [2] = column (Column with this blob)
		     */

		    String[] blobData = parts[1].split("::");
		    /*
		     * If some of blobData's values contained :: it was replaced to :\: so if we want back
		     * original value we have to replace that slash
		     */
		    for (int i1 = 0; i1 < blobData.length; i1++)
			blobData[i1] = removeSlash(blobData[i1]);

		    if (blobData[2].endsWith("   "))
			blobData[2] = blobData[2].substring(0, blobData[2].length() - 3);

		    /*
		     * Ask for raw data when target is identified by where string and all other already
		     * processed columns (except rawData pointers and null values)
		     */
		    StringBuilder whereParameters = new StringBuilder();
		    for (int i1 = 0; i1 < columns.size(); i1++) {
			if ((values.get(i1)[i] == null) || (values.get(i1)[i].startsWith("@rawData")))
			    continue;
			whereParameters.append("`" + columns.get(i1) + "` = \"" + values.get(i1)[i] + "\" AND ");
		    }
		    if (whereString != null)
			whereParameters.append(whereString);
		    else // Remove last AND
			whereParameters.setLength(whereParameters.length() - 4);
		    rawData.add(calledBy.getRawData("SELECT `" + blobData[2] + "` FROM `" + blobData[1] + "` WHERE " + whereParameters.toString(), blobData[2]));
		    parts[1] = "@rawData:" + (rawData.size() - 1);
		} else {

		    /*
		     * For some reason, PHP script adds 3 spaces add end to all values except the last one in
		     * each
		     * line. So we have to remove them.
		     */
		    if (x != (columnsAndValues.length - 1))
			parts[1] = parts[1].substring(0, parts[1].length() - 3);
		}
		/*
		 * If array parts has only length 1 it means that returned value was in this case null, what
		 * means "" for normal Strings
		 */
		if (parts.length > 1)
		    add2column(removeSlash(parts[0]), removeSlash(parts[1]), i);
		else
		    add2column(removeSlash(parts[0]), "", i);
	    }

	}
    }

    /**
     * Goes thought String and removes all slash used to identify values from operants
     * 
     * @param s
     *            String to be processed
     * @return String without unnecessary slashes
     */
    private String removeSlash(String s) {
	char[] chrs = s.toCharArray();
	boolean lastCharSlash = false;

	String returnString = "";
	for (char ch : chrs) {
	    if (!lastCharSlash && (ch == '\\')) {
		lastCharSlash = true;
		continue;
	    } else {
		lastCharSlash = false;
	    }
	    returnString += ch;
	}
	return returnString;
    }

    /**
     * Adds under column value to its index
     * 
     * @param column
     *            Name of column to add to
     * @param value
     *            Value to add
     * @param index
     *            Index of value
     */
    private void add2column(String column, String value, int index) {
	boolean columnExist = false;
	int columnIndex = 0;

	for (columnIndex = 0; columnIndex < columns.size(); columnIndex++)
	    if (columns.get(columnIndex).contentEquals(column)) {
		columnExist = true;
		break;
	    }

	if (!columnExist) {
	    // columnIndex++;
	    columns.add(column);
	    values.add(new String[maxIndex + 1]);
	}

	values.get(columnIndex)[index] = value;
    }

    /**
     * Make locally saved index greater by 1
     * 
     * @return True if index is not greater than max index, false otherwise
     */
    public boolean next() {
	currIndex++;
	if (currIndex > maxIndex) {
	    currIndex = 0;
	    return false;
	}
	return true;
    }

    /**
     * Make locally saved index lower by 1. Can't be lower than 0.
     * 
     * @return True if index is not lower than 0, false otherwise
     */
    public boolean prev() {
	if (currIndex >= 1) {
	    currIndex--;
	    return true;
	}
	return false;
    }

    /**
     * Set locally saved index to some number. Cannot be lower than 0.
     * 
     * @return True if index is not lower than 0 and greater than max index, false otherwise
     */
    public boolean setIndex(int newIndex) {
	if ((newIndex > 0) && (newIndex <= maxIndex)) {
	    currIndex = newIndex;
	    return true;
	}
	return false;
    }

    /**
     * Returns maximal index of values
     * 
     * @return maximal index of values
     */
    public int getMaxIndex() {
	return maxIndex;
    }

    /**
     * Returns array of String representing column's names
     * 
     * @return array of String representing column's names
     */
    public String[] getcolumns() {
	return (String[]) columns.toArray();
    }

    /**
     * Finds value in column by its index. As index is used locally saved index changed by this.next() and
     * this.prev() and this.setIndex()
     * 
     * @param columnName
     *            name of column to find in
     * @return value or null if index is greater than max index
     * @throws ColumnDoesNotExistException
     */
    public String getString(String columnName) throws ColumnDoesNotExistException {
	if (currIndex == -1)
	    currIndex++;
	return getString(columnName, currIndex);
    }

    /**
     * Finds value in column by its index. As index is used locally saved index changed by this.next() and
     * this.prev() and this.setIndex()
     * 
     * @param columnName
     *            name of column to find in
     * @return value or null if index is greater than max index
     * @throws ColumnDoesNotExistException
     */
    public Integer getInteger(String columnName) throws ColumnDoesNotExistException {
	return Integer.parseInt(getString(columnName));
    }

    /**
     * Finds value in column by its index. As index is used locally saved index changed by this.next() and
     * this.prev() and this.setIndex()
     * 
     * @param columnName
     *            name of column to find in
     * @return value or null if index is greater than max index or this column is not in raw data list
     * @throws ColumnDoesNotExistException
     */
    public ByteArrayOutputStream getRawData(String columnName) throws ColumnDoesNotExistException {
	if (currIndex == -1)
	    currIndex++;
	return getRawData(columnName, currIndex);
    }

    /**
     * Finds value in column by its index.
     * 
     * @param columnName
     *            name of column to find in
     * @param index
     *            index of value to find
     * @return value or null if index is greater than max index or this column is not in raw data list
     * @throws ColumnDoesNotExistException
     */
    public ByteArrayOutputStream getRawData(String columnName, int index) throws ColumnDoesNotExistException {
	/*
	 * Raw data link starts with prefix "@rawData:NUMBER_IN_rawData_LIST"
	 */
	final String link = getValue(columnName, index);
	if (!link.startsWith("@rawData:"))
	    return null;

	return rawData.get(Integer.parseInt(link.split(":")[1]));
    }

    /**
     * Finds value in column by its index. Can return "@rawData" as pointer to raw data
     * 
     * @param columnName
     *            name of column to find in
     * @param index
     *            index of value to find
     * @return value from values list or null if index is greater than max index
     * @throws ColumnDoesNotExistException
     */
    public String getValue(String columnName, int index) throws ColumnDoesNotExistException {
	if (index > maxIndex)
	    return null;

	boolean columnExist = false;
	int columnIndex = 0;

	for (columnIndex = 0; columnIndex < columns.size(); columnIndex++)
	    if (columns.get(columnIndex).contentEquals(columnName)) {
		columnExist = true;
		break;
	    }

	if (!columnExist)
	    throw new ColumnDoesNotExistException("column '" + columnName + "' does not exist in this respond");
	return values.get(columnIndex)[index];
    }

    /**
     * Finds value in column by its index. It is a "@rawData" pointer, tries to return a String made from
     * rawData
     * 
     * @param columnName
     *            name of column to find in
     * @param index
     *            index of value to find
     * @return ByteArrayOutputStream converted into String if value starts wit a "@rawData" pointer or value
     *         from values list or null if index is greater than max index
     * @throws ColumnDoesNotExistException
     */
    public String getString(String columnName, int index) throws ColumnDoesNotExistException {
	if (index > maxIndex)
	    return null;

	boolean columnExist = false;
	int columnIndex = 0;

	for (columnIndex = 0; columnIndex < columns.size(); columnIndex++)
	    if (columns.get(columnIndex).contentEquals(columnName)) {
		columnExist = true;
		break;
	    }

	if (!columnExist)
	    throw new ColumnDoesNotExistException("column '" + columnName + "' does not exist in this respond");

	String returnValue = values.get(columnIndex)[index];
	if (returnValue.startsWith("@rawData"))
	    returnValue = getRawData(columnName, index).toString();
	return returnValue;
    }

    /**
     * Finds value in column by its index. As index is used locally saved index changed by this.next() and
     * this.prev() and this.setIndex()
     * 
     * @param columnName
     *            name of column to find in
     * @return true if numeric value is larger than 0, false otherwise
     * @throws ColumnDoesNotExistException
     */
    public Boolean getBoolean(String columnName) throws ColumnDoesNotExistException {
	if (getInteger(columnName) > 0)
	    return true;
	return false;
    }

    /**
     * Finds value in column by its index. As index is used locally saved index changed by this.next() and
     * this.prev() and this.setIndex()
     * 
     * @param columnName
     *            name of column to find in
     * @return found value or null if index is greater than max index
     * @throws NumberFormatException
     * @throws ColumnDoesNotExistException
     */
    public Double getDouble(String columnName) throws NumberFormatException, ColumnDoesNotExistException {
	return Double.parseDouble(getString(columnName));
    }

    /**
     * Finds value in column by its index. As index is used locally saved index changed by this.next() and
     * this.prev() and this.setIndex()
     * 
     * @param columnName
     *            name of column to find in
     * @return found value or null if index is greater than max index
     * @throws NumberFormatException
     * @throws ColumnDoesNotExistException
     */
    public Long getLong(String columnName) throws NumberFormatException, ColumnDoesNotExistException {
	return Long.parseLong(getString(columnName));
    }
}
