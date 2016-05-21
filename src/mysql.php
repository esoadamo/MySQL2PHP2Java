<?PHP

/*
Here are database settings:
*/
$user_name = "";
$password = "";
$database = "";
$server = "";

//For a security reason, please set up an acess password to not allow everybody using this page
$acess_pass = "";

/*
How to acess: http POST www.yourserver.com/this.php, as parameters enter p=acess_pass&sql=your_sql_command

If you want a binary data only, then you shouhld acces www.yourserver.com/this.php?p=acess_pass&getRaw=t&tcollum=name_of_collum_with_blob&sql=your_sql_command
Return codes:
0 - OK
2 - No command specified
3 - Wrong acess password
4 - Testing just connection to server was successful
5 - Testing connection to MySQL was successful
*/

//Check if client just want to test connection to this script
if( isset( $_POST["testing"]) &&  ( $_POST["testing"] == "testPHP" )) {

	print("4: Connection to PHP script works. Good work m8s!". "<BR>");
	exit (4);

}

//Check if client just want to test connection to the database
if( isset( $_POST["testing"]) &&  ( $_POST["testing"] == "testMySQL" )) {

	$connection = mysqli_connect($server, $user_name, $password, $database);

	if (mysqli_connect_errno()) {
		print("<BR><BR>Connect failed: " . mysqli_connect_error() . "<BR>");
		exit (255);
	}

	mysqli_close($connection);
	print("5: Connection to MySQL works. Good work m8s!". "<BR>");
	exit (5);
}

//Check if we have command parameter
if( !isset( $_POST["sql"]) ||  ( $_POST["sql"] == "" )) {

	print("2: Welcome to PHP <-> MySQl server operator.". "<BR>");
	print("This webpage not acessible for web browsers, please use application with in-build support.". "<BR>");
	exit (2);

}

//Check password
if( (!isset( $_POST["p"]) && ($acess_pass != "")) ||  ( $_POST["p"] !=  $acess_pass)) {

	print("3: Wrong password". "<BR>");
	exit (3);

}

$connection = mysqli_connect($server, $user_name, $password, $database);

	mysqli_query($connection, "SET character_set_results = 'utf8', character_set_client = 'utf8', character_set_connection = 'utf8', character_set_database = 'utf8', character_set_server = 'utf8'");

//Client wants to get only raw data
if( isset( $_POST["getRaw"]) &&  ( $_POST["getRaw"] == "t" )) {

	$tCollum =  $_POST["tcollumn"];

	$SQL =  $_POST["sql"];

	$result = mysqli_query($connection, $SQL);

	$db_field = mysqli_fetch_assoc($result);

	$respond = print_r($db_field[$tCollum], true);

	print $respond;

	mysqli_close($connection);

	exit(6);
}

	$SQL =  $_POST["sql"];
	$result = mysqli_query($connection, $SQL);

	print "0: Results for " .  $_POST["sql"] . "<BR>";

	/*Find blob columns */
	$blob_columns = array(); //Index of columns with blobs
	$blob_columns_tables = array(); //Tables where are blobs stored
	$blob_columns_names = array(); //Names of columns with blobs
	for ($i=0; $i < mysqli_num_fields($result); $i++) {
		$finfo = $result->fetch_field_direct($i);
    		$type  = $finfo->type;
		if(($type == MYSQLI_TYPE_BLOB) || ($type == MYSQLI_TYPE_TINY_BLOB) || ($type == MYSQLI_TYPE_MEDIUM_BLOB) || ($type == MYSQLI_TYPE_LONG_BLOB)){
			$blob_columns[$i] = $i;
			$table = $finfo->table;
			$column = $finfo->name;
			$blob_columns_tables[$i] = $table;
			$blob_columns_names[$i] = $column;
		}
	}

	while ( $db_field = mysqli_fetch_assoc($result) ) {

		$column = 0;
		foreach ($db_field as &$value) {
			$thisIsBlob = false;
			/* If current column has same number as column change its value to "blob", so user does not have to download 
			   all blobs twice. (He will have to downloads blob as raw data again) */
			foreach ($blob_columns as &$blobColumn)
				if($column == $blobColumn){
					//If this values is a blob, we will change it to blob::TableWithBlob::ColumnWithBlob
					$blobTable = $blob_columns_tables[$blobColumn];
					$blobTable = str_replace("::", ":\\:", $blobTable);

					$blobColumnName = $blob_columns_names[$blobColumn];
					$blobColumnName = str_replace("::", ":\\:", $blobColumnName);

					$value = "blob" . "::" . $blobTable . "::" . $blobColumnName ;
					$thisIsBlob = true;
					break;
				}
			if(!$thisIsBlob) {
				$value = str_replace("\\", "\\\\", $value);
				
				//We have to change some control characters in values so it will not disturb splitting in target applications
				$brackets   = array("[", "]", "(", ")", "<BR>", "blob");
				$replacements   = array("\\[", "\\]", "\\(", "\\)", "<B\\R>", "\\blob");
    				$value = str_replace($brackets, $replacements, $value);
			}
			$column++;
		}

		$respond = print_r($db_field, true);

		$brackets   = array(" [", "=> ");
		$replacements   = array("], [", "=> [");

    		$respond = str_replace($brackets, $replacements, $respond);
		print $respond;
		print "<BR>";
	}


	mysqli_close($connection);
	
	exit(0);
?>


