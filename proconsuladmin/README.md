The proconsuladmin project provides a (currently very thin) web-based UI for managing the dataabse tables which control the behavior of a deployed Proconsul instance.  

The initial UI is exceedgingly simplistic and (imo) profoundly unsightly, but it is functional.  Time and support from some UI/UX specialists later in 2017 will hopefully yield a more pleasant presentation layer, but for the moment, the UI at least provides a better means for reviewing and managing Proconsul than manual manipulation of the database.

For the time being, the proconsuladmin tool uses explicit delegation to individual users for authorization -- authorized administrators are designated by adding their eppns (or whatever value will appear in the "remote user" value after the user authenticates) to a comma-separated list in the embedded properties file.
