# Roadside Assistance Dispatch System - with MySQL database

## What changed
| Before (prototype) | Now |
|---|---|
| Providers hard-coded inside `MainFrame` | Providers live in the database (`service_provider`, `mechanic`, `tow_truck`, `fuel_supplier`) |
| Request id always 1001 | Database generates 1001, 1002, ... |
| First free provider is sent, whatever the problem | The database picks the provider type that fits (see rules below) |
| Nothing saved when the window closes | Drivers, vehicles, requests, invoices, audit log and tracking are stored |
| Status text only | Full tracking timeline (Received, Assigned, Arrived, Completed) |

### Which provider is sent?
| Problem | Car can be driven | Car cannot be driven |
|---|---|---|
| Flat Tyre | Mechanic | Mechanic (fixed on the spot) |
| Engine Failure | Mechanic | Tow truck |
| Empty Fuel Tank | Fuel van | Fuel van |
| Accident | Tow truck | Tow truck |

## Setup (5 steps)
1. **Install MySQL 8.0 or newer** and make sure it is running.
2. **Create the database.** From this folder's parent run:
   `mysql -u root -p < roadside_db.sql`
   (This also creates the login `roadside_app` the Java app uses.)
3. **Get the JDBC driver.** Download *MySQL Connector/J* (Platform Independent zip) from
   https://dev.mysql.com/downloads/connector/j/ , unzip it and copy the
   `mysql-connector-j-x.x.x.jar` file into the `lib` folder here.
4. **Check `db.properties`.** Change the password or port only if yours differ.
5. **Compile and run** from inside this folder:

   Windows:
   ```
   javac -cp ".;lib/*" *.java
   java  -cp ".;lib/*" Main
   ```
   Linux / Mac:
   ```
   javac -cp ".:lib/*" *.java
   java  -cp ".:lib/*" Main
   ```

Optional check without the screen: `java -cp ".;lib/*" DbSmokeTest` (use `:` instead of `;` on Linux/Mac).

## How the files fit together
```
MainFrame  (screen)
   |  uses
DispatchManager  (dispatch / arrived / complete / cancel)
   |  uses
RequestRepository  (the only file with SQL; calls stored procedures)
   |  uses
Database  (opens connections using db.properties)
   |
MySQL: roadside_db  (tables, views, triggers, procedures)
```

## Common problems
* **"Could not connect to the database"** - MySQL not running, script not run, or wrong values in `db.properties`.
* **"No suitable driver found" / ClassNotFoundException** - the Connector/J jar is not in `lib`, or `-cp` is missing `lib/*`.
* **"Access denied for user"** - password in `db.properties` differs from the one in `roadside_db.sql`.
* **"Public Key Retrieval is not allowed"** - keep `allowPublicKeyRetrieval=true` in the URL.
