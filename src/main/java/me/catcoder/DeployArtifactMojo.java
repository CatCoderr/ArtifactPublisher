package me.catcoder;

import com.vk.api.sdk.client.TransportClient;
import com.vk.api.sdk.client.VkApiClient;
import com.vk.api.sdk.client.actors.UserActor;
import com.vk.api.sdk.exceptions.ApiException;
import com.vk.api.sdk.exceptions.ClientException;
import com.vk.api.sdk.objects.docs.Doc;
import com.vk.api.sdk.objects.docs.responses.DocUploadResponse;
import com.vk.api.sdk.objects.docs.responses.GetUploadServerResponse;
import org.apache.maven.artifact.Artifact;
import org.apache.maven.artifact.metadata.ArtifactMetadata;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.project.MavenProject;
import org.apache.maven.project.artifact.ProjectArtifactMetadata;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.time.Duration;
import java.time.Instant;
import java.util.Date;
import java.util.Properties;
import java.util.stream.Collectors;


/**
 * Represents MOJO deploy task.
 *
 * @author CatCoder
 */
@Mojo(name = "deploy-artifact", defaultPhase = LifecyclePhase.PACKAGE)
public class DeployArtifactMojo extends AbstractMojo {

    //Date format
    public static final DateFormat DATE_FORMAT = new SimpleDateFormat("yyyy.MM.dd G 'at' HH:mm:ss ");

    //VK API constants
    public static final TransportClient TRANSPORT_CLIENT = new ModifiedHttpClient();
    public static final VkApiClient API_CLIENT = new VkApiClient(TRANSPORT_CLIENT);

    // Inject Maven project
    @Parameter(defaultValue = "${project}", readonly = true, required = true)
    private MavenProject project;

    //Deploy properties
    @Parameter(name = "accessToken", required = true, readonly = true)
    private String accessToken;
    @Parameter(name = "receiverId", required = true, readonly = true)
    private Integer receiverId;
    @Parameter(name = "senderId", required = true, readonly = true)
    private Integer senderId;


    /**
     * Execute deploy.
     *
     * @throws MojoExecutionException - if we have some VkAPI/artifact copy troubles.
     */
    public void execute() throws MojoExecutionException {

        Artifact artifact = project.getArtifact();
        File pomFile = project.getFile();

        ArtifactMetadata metadata = new ProjectArtifactMetadata(artifact, pomFile);
        artifact.addMetadata(metadata);

        File artFile = artifact.getFile();

        getLog().debug("File: " + artFile.getName());


        File renamed;
        try {
            renamed = copyAndRename(artFile);
        } catch (IOException e) {
            throw new MojoExecutionException(this, "Cannot create artifact copy", e.getMessage());
        }
        getLog().debug("File successfully copied " + renamed.getName());

        UserActor sender = new UserActor(senderId, accessToken);

        try {
            Instant instant = Instant.now();
            GetUploadServerResponse uploadServer = API_CLIENT.docs().getUploadServer(sender).execute();
            DocUploadResponse response = API_CLIENT.upload().doc(uploadServer.getUploadUrl(), renamed).execute();

            getLog().info(String.format("File uploaded (%s)", Duration.between(Instant.now(), instant)));

            Doc doc = API_CLIENT.docs().save(sender, response.getFile()).execute().get(0);

            String attachId = "doc" + doc.getOwnerId() + "_" + doc.getId();

            getLog().debug("Attachment: " + attachId);

            API_CLIENT.messages()
                    .send(sender)
                    .userId(receiverId)
                    .attachment(attachId)
                    .message("Сборка сгенерирована: " + DATE_FORMAT.format(new Date()))
                    .execute();

            getLog().info(String.format("Deploy completed (%s)", Duration.between(Instant.now(), instant)));

        } catch (ApiException | ClientException ex) {
            throw new MojoExecutionException(ex.getMessage(), ex);
        }

    }


    /**
     * Creates new file and copy jar data to new file
     *
     * @param file - jar
     * @return renamed file
     * @throws IOException - if copy failed
     */
    private File copyAndRename(File file) throws IOException {
        File renamed = new File(project.getBuild().getOutputDirectory(), file.getName().split("\\.")[0] + ".jarr");

        Files.copy(file.toPath(), renamed.toPath());

        return renamed;
    }

}
