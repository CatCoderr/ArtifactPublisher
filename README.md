# ArtifactPublisher
Publish build (jar) artifact as VK message


## Configuring

Adding repository to your pom.xml:
```xml
<repositories>
	<repository>
		   <id>jitpack.io</id>
		   <url>https://jitpack.io</url>
	 </repository>
</repositories>
```

Adding plugin to your pom.xml:
```xml
<plugin>
  <groupId>com.github.CatCoderr</groupId>
  <artifactId>ArtifactPublisher</artifactId>
  <version>v1.2</version>
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

To secure your access token, your must configure maven-jar-plugin settings to the following:
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

