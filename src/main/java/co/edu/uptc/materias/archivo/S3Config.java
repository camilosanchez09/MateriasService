package co.edu.uptc.materias.archivo;

import java.net.URI;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.util.StringUtils;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.S3Configuration;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;

@Configuration
@EnableConfigurationProperties(StorageProperties.class)
public class S3Config {

    @Bean
    public S3Client s3Client(StorageProperties props) {
        var builder = S3Client.builder()
                .region(Region.of(props.region()))
                .forcePathStyle(props.pathStyle());
        if (StringUtils.hasText(props.endpoint())) {
            builder.endpointOverride(URI.create(props.endpoint()));
        }
        return builder.build();
    }

    /** Firma contra el endpoint que alcanza el navegador (en local, el de Docker puede no serlo). */
    @Bean
    public S3Presigner s3Presigner(StorageProperties props) {
        String endpoint = StringUtils.hasText(props.publicEndpoint()) ? props.publicEndpoint() : props.endpoint();
        var builder = S3Presigner.builder()
                .region(Region.of(props.region()))
                .serviceConfiguration(S3Configuration.builder().pathStyleAccessEnabled(props.pathStyle()).build());
        if (StringUtils.hasText(endpoint)) {
            builder.endpointOverride(URI.create(endpoint));
        }
        return builder.build();
    }
}
