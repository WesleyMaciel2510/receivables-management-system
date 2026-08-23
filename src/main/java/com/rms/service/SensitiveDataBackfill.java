package com.rms.service;

import com.rms.model.Client;
import com.rms.model.Receivable;
import com.rms.repository.ClientRepository;
import com.rms.repository.ReceivableRepository;
import com.rms.security.FieldEncryptionService;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.transaction.annotation.Transactional;

@Configuration
public class SensitiveDataBackfill {
    private static final Logger log = LoggerFactory.getLogger(SensitiveDataBackfill.class);

    @Bean
    CommandLineRunner backfillSensitiveData(ClientRepository clients, ReceivableRepository receivables,
            BlindIndexService indexes, FieldEncryptionService encryption) {
        return args -> run(clients, receivables, indexes, encryption);
    }

    @Transactional
    void run(ClientRepository clients, ReceivableRepository receivables, BlindIndexService indexes,
            FieldEncryptionService encryption) {
        List<Client> clientRows = clients.findAll();
        for (Client client : clientRows) {
            client.updateSearchIndexes(indexes.document(client.getCpfCnpj()), indexes.email(client.getEmail()));
            clients.save(client);
        }
        List<Receivable> receivableRows = receivables.findAll();
        receivableRows.forEach(receivables::save);
        log.info("Sensitive data backfill verified {} clients and {} receivables", clientRows.size(), receivableRows.size());
    }
}
