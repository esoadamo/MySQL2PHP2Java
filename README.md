## MySQL2PHP(*2Java*) ##
Access you MySQL database without direct connection do MySQL database.

What's this?
============
OK, here is your problem: 

 1. You have a MySQL database.
 2. You have a http server with connection to that database
 3. For security reasons, your MySQL is set to allow access only from localhost (or another whitelist)
 4. You want to access from your application on your laptop.
 5. You travel a lot with your laptop and connect to many networks, so your IP is often changing, so whitelist is impractical for this (or you cannot change it)

And here is the solution:
Just put this [PHP script](https://github.com/esoadamo/MySQL2PHP2Java/blob/master/src/mysql.php) on your http server, configure it and you are ready to go. All SQL request you will make you can send to PHP script, which will redirect them to MySQL database, which will send respond to PHP script, which will redirect it back to you in defined format.

Features & To-do
=======
You can:

 - Send & receive any text based data (varchar, integer, text.... all except binary data, e.g. blob)
 - Receive binary data (like blobs)
 - Test if connection works (to PHP and/or MySQL)
 - Set-up PHP script's password protection
 - You don't have to put your password to database into client application, just into PHP script

You can not, but it's planned:

 - Send binary data (like blobs)
 - Limit failed PHP script login attempts

PHP part
=======

Configuration
-------------
Configuration can be found in the top of [scr/mysql.php](https://github.com/esoadamo/MySQL2PHP2Java/blob/master/src/mysql.php) file.

$user\_name = "mysql"; //User accessing database
 
$password = "1234"; //Password for user

$database = "testDatabase"; //Database to access to

$server = "localhost"; //Server, where is MySQL running

$acess_pass = "pass"; //Password required to execute sql commands. Setting this is optional.


Respond codes
-------
**0** - everything is OK. Shown when using sql parameter
**1** - target database was not found
**2** - you have executed script, but with no sql parameter. Normally shown when somebody access direct in his web browser
**3** - your password is wrong
**4** - connection to PHP script works fine (used with testPHP parameter)
**5** - connection to MySQL database works fine (used with testMySQL parameter)
 
API
-------
Accessing to your PHP script is made by http POST on your PHP script's URL (e.g. http://example.com/mysql.php) and every time you will execute PHP script, first returned character will be respond code.

**Testing connection**
For testing connection you do not have to input password, because it will not show any sensitive data.
For testing is used parameter *testing*

    wget --post-data="testing=testPHP" http://example.com/mysql.php
    
    wget --post-data="testing=testMySQL" http://example.com/mysql.php
First example will return status 4 if was successful, second will return status 5. Any other status means fail.

**Executing SQL command**
For executing any SQL command is used parameter *sql* with normal SQL syntax. If you have password on your PHP script, this is where you use it (as parameter *p*)
Selecting name of users in database

 1. if you have no password specified:

    wget --post-data="sql=SELECT name FROM users" "http://example.com/mysql.php"

 2. if  you have password specified you will just add parameter *p* with you PHP script's password:

    wget --post-data="sql=SELECT name FROM users&p=yourPassword" "http://example.com/mysql.php"
    
Updating tables in your database is pretty the same:

    wget --post-data="sql=UPDATE users SET name = Admin WHERE id = 1" "http://example.com/mysql.php"
    

**Value modification**
To make things easier, you can see where your value starts and ends by looking for operation characters. So, if script founds one of operation character in column name or it's value, it will place \ before it to make parsing easier. That is the reason why you have to remove every odd \ from parsed column name or value and why script cannot post binary data directly with other results.

**Getting binary data**
Because of **Value modification** script cannot give you binary data directly with other results. So, if script finds  binary data (by getting column type number) it will replace it with following string:

    blob::tableWithBlob::columnWithBlob
*Warning: MySQL uses same type number for TEXT and BLOB, so TEXT is also replaced*

And here comes parameter *getRaw* and *tcollumn*. If *getRaw* is set to *t*, script will execute SQL (*sql* parameter) but only return byte value of column *tcollumn* in first returned row.

Let say we want to get user's photo and name:

     wget --post-data="sql=SELECT name, photo FROM users WHERE id = 1&p=yourPassword" "http://example.com/mysql.php"

Aftter parsing you will find that there are two columns:

 1. name with value *Admin*
 2. photo with value *blob::users::photo*

To get the photo we need to send second command, which will return unchanged bytes of photo:

    wget --post-data="sql=SELECT photo FROM users WHERE id = 1&p=yourPassword&getRaw=t&tcollumn=photo" "http://example.com/mysql.php"

Java part
=======
Lucky for you, most of Java part is automatic and described in [documentation](https://github.com/esoadamo/MySQL2PHP2Java/tree/master/doc) . I will write down just the most basic things you should now:
Depend if you have password on your script, initialization is run by:

    MySQL2PHP2Java conn = new MySQL2PHP2Java("http://example.com/mysql.php");
or

    MySQL2PHP2Java conn = new MySQL2PHP2Java("http://example.com/mysql.php", "password");

then to print all names of users you should use
	

    MySQLRespond respond = conn.executeSQL("SELECT name FROM users");
    while(respond.next())
      System.out.println(respond.getString("name"););

Do you like my work?
=======
If sou, why not consider [donation](https://www.paypal.com/cgi-bin/webscr?cmd=_s-xclick&hosted_button_id=QM6PJFRG3A5QU) to help me make more cool and/or useful stuff


