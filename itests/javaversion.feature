Feature: java version control

  Scenario: java run non existent //java
    When command('jbang --verbose java4321.java')
    Then match err contains "No suitable JDK was found for requested version: 4321"


  Scenario: java run with explicit java 8
    When command('jbang --verbose --java 8 java4321.java')
    Then match err !contains "No suitable JDK was found for requested version: 4321"