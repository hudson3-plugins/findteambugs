find-team-bugs
==============

Scan a Hudson home with Teams and check for malformed job names, orphan jobs or missing jobs.

    Usage: java -jar findteambugs.jar [ SWITCHES ] HUDSON_HOME
           where HUDSON_HOME is a path to valid Hudson home
                              to scan for team-related bugs
           where SWITCHES are:
             -v | --version   Print version and exit (optional)
             -q | --quiet     Don't write to standard output (optional)
             -n | --nopublic  Flag public jobs as errors (optional)
             -f | --fix       Fix team-related bugs (optional)
                              Removes orphan jobs and badly named jobs
                              from disk, and missing jobs from teams.xml
                              Back up Hudson home before fixing.
           Run with no switches to see bugs detected.
