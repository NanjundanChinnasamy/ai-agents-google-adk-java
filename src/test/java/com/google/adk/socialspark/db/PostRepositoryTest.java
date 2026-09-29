package com.google.adk.socialspark.db;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

public class PostRepositoryTest {
    private File tempDb;

    @BeforeEach
    void setUp() throws Exception {
        tempDb = File.createTempFile("test_social_spark", ".db");
        tempDb.deleteOnExit();
        PostRepository.setDbPath(tempDb.getAbsolutePath());
    }

    @AfterEach
    void tearDown() {
        if (tempDb != null && tempDb.exists()) {
            tempDb.delete();
        }
    }

    @Test
    void testSaveAndListPosts() {
        PostRecord saved = PostRepository.savePost(
                "LinkedIn",
                "Hello world from Java ADK!",
                "https://linkedin.com/post/123",
                "/tmp/image.png",
                "https://storage.googleapis.com/bucket/image.png"
        );

        assertThat(saved.id()).isGreaterThan(0);
        assertThat(saved.platform()).isEqualTo("LinkedIn");
        assertThat(saved.text()).isEqualTo("Hello world from Java ADK!");
        assertThat(saved.postUrl()).isEqualTo("https://linkedin.com/post/123");

        List<PostRecord> list = PostRepository.listPosts(10);
        assertThat(list).hasSize(1);
        assertThat(list.get(0).id()).isEqualTo(saved.id());
        assertThat(list.get(0).text()).isEqualTo("Hello world from Java ADK!");
    }
}
