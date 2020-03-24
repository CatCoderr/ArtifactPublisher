# ArtifactPublisher
[![Maven Central](https://img.shields.io/maven-central/v/io.github.catcoderr/artifact-publisher-plugin.svg)]()  
Publish build (jar) artifact as VK message


## Configuring

Adding plugin to your pom.xml in build process:
```xml
<plugin>
  <groupId>io.github.catcoderr</groupId>
  <artifactId>artifact-publisher-plugin</artifactId>
  <version>1.2</version>
  <executions>
      <execution>
          <phase>package</phase>
          <goals>
              <goal>deploy-artifact</goal>
          </goals>
      </execution>
  </executions>
  <configuration>
      <accessToken>YOUR_ACCESS_TOKEN</accessToken>
      <senderId>YOUR_VK_ID</senderId>
      <receiverId>CUSTOMER_VK_ID</receiverId>
  </configuration>
</plugin>
```

#### Important!

To secure your access token, you have to set your maven-jar-plugin settings to the following:
```xml
<plugin>
    <groupId>org.apache.maven.plugins</groupId>
    <artifactId>maven-jar-plugin</artifactId>
    <version>2.3.1</version>
    <configuration>
        <archive>
             <!-- Disable including pom.xml in jar !-->
            <addMavenDescriptor>false</addMavenDescriptor>
        </archive>
    </configuration>
</plugin>
```

