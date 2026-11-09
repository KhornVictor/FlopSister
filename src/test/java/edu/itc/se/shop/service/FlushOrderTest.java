package edu.itc.se.shop.service;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import edu.itc.se.shop.domain.Tag;
import edu.itc.se.shop.repository.TagRepository;

@SpringBootTest
@ActiveProfiles("test")
class FlushOrderTest {
    @Autowired
    private TagRepository tags;

    @BeforeEach
    void createSaleTag() {
        tags.deleteAll();
        tags.saveAndFlush(new Tag("sale"));
    }

    @Test
    @Transactional
    void insertBeforeDeleteViolatesUniqueConstraint() {
        Tag saleTag = tags.findByName("sale").orElseThrow();

        assertThatThrownBy(() -> {
            tags.delete(saleTag);
            tags.save(new Tag("sale"));
            tags.flush();
        }).isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    @Transactional
    void flushingDeleteBeforeInsertWorks() {
        Tag saleTag = tags.findByName("sale").orElseThrow();

        assertThatCode(() -> {
            tags.delete(saleTag);
            tags.flush();
            tags.save(new Tag("sale"));
            tags.flush();
        }).doesNotThrowAnyException();
    }
}
