package com.example.pt.lab3.repository;

import com.example.pt.lab3.domain.PlayerInstance;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.UUID;

@Repository
public interface PlayerRepository extends JpaRepository<PlayerInstance, UUID> {
}
