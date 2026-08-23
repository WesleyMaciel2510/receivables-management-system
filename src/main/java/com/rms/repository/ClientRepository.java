package com.rms.repository;

import com.rms.model.Client;
import java.util.UUID;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface ClientRepository extends JpaRepository<Client, UUID> {
    boolean existsByCpfCnpjHash(String cpfCnpjHash);
    boolean existsByEmailHash(String emailHash);

    @Query(value = "SELECT * FROM clients WHERE deleted_at IS NOT NULL ORDER BY deleted_at DESC", nativeQuery = true)
    List<Client> findArchived();
}
