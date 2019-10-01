# Documentation
 - https://sterl.org/2019/09/quartz-scheduler-integration-in-java-jee

# How to run
- setup a postgresql db
- Add jdbc pool and resource for quartz named: `jdbc/quartz-datasource`
    - Non Transactional Connections has to be set to true
- Add jdbc pool and resource for the app named: `jdbc/app-datasource`
- create a concurrent executor with the name: `concurrent/quartz-executor`
    - Queue Capacity: **0**
    - Longing Running to **true**
- copy war into the autodeploy // or use the IDE
- add `org.quartz.scheduler.skipUpdateCheck` to **true** to the jvm-options
- add `quartz.node.id` with a unique id to each domain you start as jvm-options

# Links
- https://devsoap.com/injecting-cdi-managed-beans-into-quarz-jobs/
- https://stackoverflow.com/questions/4812796/can-i-use-cdi-injection-into-quartz-scheduler-jobs
- https://flylib.com/books/en/2.65.1/configuring_datasources_for_jobstorecmt.html
- https://deltaspike.apache.org/documentation/scheduler.html
- https://rmannibucau.wordpress.com/2015/03/02/cdi-and-instance-3-pitfalls-you-need-to-know/