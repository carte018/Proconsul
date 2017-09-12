# Database Configuration

The file `proconsul.db.schema` contains MySQL commands necessary to
prepare a MySQL database for use by Proconsul.  Run it as a normal SQL
script in your MySQL server to set up the requisite "proconsul"
database and the requisite tables within the database for proconsul
use.

You will need to establish two separate mysql users -- one with
read-only rights to the "proconsul" database and all its tables, and
one with read-write access to the database.
