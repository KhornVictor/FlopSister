package edu.itc.se.shop.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import edu.itc.se.shop.domain.Tag;

public interface TagRepository extends JpaRepository<Tag, Long> {
    Optional<Tag> findByName(String name);
}
