package org.secondbase.flags;

import static org.junit.Assert.assertEquals;

import com.amazonaws.auth.profile.ProfileCredentialsProvider;
import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.AmazonS3ClientBuilder;
import com.amazonaws.services.s3.model.AmazonS3Exception;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import org.junit.After;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

/**
 * Integration test for secret fetching from AWS S3.
 *
 * Requires environment variables:
 *
 * AWS_PROFILE - The aws profile containing access credentials to S3
 * AWS_S3_BUCKET - The bucket used for testing
 * AWS_S3_KEY_PREFIX - The prefix for keys stored (in the bucket chosen)
 * AWS_S3_CREATE_BUCKET - Set to "true" if the test should create the bucket if it does not exist
 *                        (optional, default="false")
 *
 * @author acidmoose
 */
public class SecretS3IT {

    // The path for the tests. Includes UUID. Use for individual tests.
    private String s3path;
    private String bucket;
    private AmazonS3 s3Client;
    private Flags flags;
    private List<String> secrets = new ArrayList<>();

    @Flag(name="teststring")
    private static String testString;

    @BeforeClass
    public static void verifyEnv() {
        verifyEnvironmentVariable("AWS_PROFILE");
        verifyEnvironmentVariable("AWS_S3_BUCKET");
        verifyEnvironmentVariable("AWS_S3_KEY_PREFIX");
    }

    private static void verifyEnvironmentVariable(final String variable) {
        if (System.getenv(variable) == null) {
            throw new IllegalStateException(variable + " not found in environment variables.");
        }
    }

    @Before
    public void setup() {
        final ProfileCredentialsProvider credentialsProvider
                = new ProfileCredentialsProvider(System.getenv("AWS_PROFILE"));
        s3Client = AmazonS3ClientBuilder.standard().withCredentials(credentialsProvider).build();
        bucket = System.getenv("AWS_S3_BUCKET");
        if (! s3Client.doesBucketExist(bucket)) {
            if (System.getenv("AWS_S3_CREATE_BUCKET") == null
                    || ! Boolean.parseBoolean(System.getenv("AWS_S3_CREATE_BUCKET"))) {
                throw new IllegalStateException("Bucket does not exist and not allowed to create.");
            }
            s3Client.createBucket(bucket);
        }
        final String testUUID = UUID.randomUUID().toString();
        s3path = System.getenv("AWS_S3_KEY_PREFIX") + "/" + testUUID;

        flags = new Flags();
        flags.loadOpts(this.getClass());
        flags.setS3CredentialsProvider(credentialsProvider);
    }

    @After
    public void tearDown() {
        for (final String secret : secrets) {
            s3Client.deleteObject(bucket, secret);
        }
    }

    @Test
    public void getStringSecret() {
        // put some secrets there
        final String secretStringFile = "secretStringFile";
        final String secretStringValue = "secretValue";
        putSecret(s3path + "/" + secretStringFile, secretStringValue);

        flags.parse(new String[]{
                "--teststring",
                "secret:s3://" + bucket + "/" + s3path + "/" + secretStringFile});
        assertEquals(secretStringValue, testString);
    }

    @Test(expected = AmazonS3Exception.class)
    public void secretNotFound() {
        flags.parse(new String[]{
                "--teststring",
                "secret:s3://" + bucket + "/" + s3path + "/nonExistingSecretFile"});
    }

    @Test
    public void s3PathNotSecretPath() {
        flags.parse(new String[]{
                "--teststring",
                "s3://" + bucket + "/" + s3path + "/nonExistingSecretFile"});
        // Make sure we're not trying to get a secret, but treat it as a regular String
        assertEquals("s3://" + bucket + "/" + s3path + "/nonExistingSecretFile", testString);
    }

    // put a secret in s3 and store for later so we can clean up
    private void putSecret(final String key, final String value) {
        s3Client.putObject(bucket, key, value);
        secrets.add(key);
    }
}
