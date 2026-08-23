package com.rms.service;

import com.rms.dto.ClientRequest;
import com.rms.dto.ClientResponse;
import com.rms.model.Client;
import com.rms.repository.ClientRepository;
import com.rms.repository.ReceivableRepository;
import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class ClientService {
    private final ClientRepository clientRepository;
    private final ReceivableRepository receivableRepository;
    private final BlindIndexService blindIndexService;

    public ClientService(ClientRepository clientRepository, ReceivableRepository receivableRepository,
            BlindIndexService blindIndexService) {
        this.clientRepository = clientRepository;
        this.receivableRepository = receivableRepository;
        this.blindIndexService = blindIndexService;
    }

    public ClientResponse create(ClientRequest request) {
        String cpfCnpj = normalizeDocument(request.cpfCnpj());
        if (!isValidDocument(cpfCnpj)) {
            throw new IllegalArgumentException("CPF/CNPJ must contain 11 or 14 digits");
        }
        if (clientRepository.existsByCpfCnpjHash(blindIndexService.document(cpfCnpj))) {
            throw new ConflictException("CPF/CNPJ is already registered");
        }
        String email = request.email().trim().toLowerCase();
        if (clientRepository.existsByEmailHash(blindIndexService.email(email))) {
            throw new ConflictException("Email is already registered");
        }
        Client client = new Client(request.name().trim(), cpfCnpj, email, request.phone().trim(), request.status());
        client.updateSearchIndexes(blindIndexService.document(cpfCnpj), blindIndexService.email(email));
        return ClientResponse.from(clientRepository.save(client));
    }

    @Transactional(readOnly = true)
    public List<ClientResponse> list() {
        return clientRepository.findAll().stream().map(ClientResponse::from).toList();
    }

    @Transactional(readOnly = true)
    public List<ClientResponse> listArchived() {
        return clientRepository.findArchived().stream().map(ClientResponse::from).toList();
    }

    public Client get(UUID id) {
        return clientRepository.findById(id).orElseThrow(() -> new ResourceNotFoundException("Client not found: " + id));
    }

    public ClientResponse update(UUID id, ClientRequest request) {
        Client client = get(id);
        String cpfCnpj = normalizeDocument(request.cpfCnpj());
        if (!isValidDocument(cpfCnpj)) {
            throw new IllegalArgumentException("CPF/CNPJ must contain 11 or 14 digits");
        }
        if (!cpfCnpj.equals(client.getCpfCnpj())
                && clientRepository.existsByCpfCnpjHash(blindIndexService.document(cpfCnpj))) {
            throw new ConflictException("CPF/CNPJ is already registered");
        }
        String email = request.email().trim().toLowerCase();
        if (!email.equals(client.getEmail()) && clientRepository.existsByEmailHash(blindIndexService.email(email))) {
            throw new ConflictException("Email is already registered");
        }
        client.update(request.name().trim(), cpfCnpj, email, request.phone().trim(), request.status());
        client.updateSearchIndexes(blindIndexService.document(cpfCnpj), blindIndexService.email(email));
        return ClientResponse.from(clientRepository.save(client));
    }

    public void delete(UUID id) {
        Client client = get(id);
        if (receivableRepository.existsByClientId(id)) {
            throw new ConflictException("Client has dependent receivables");
        }
        clientRepository.delete(client);
    }

    private String normalizeDocument(String document) {
        return document.replaceAll("\\D", "");
    }

    private boolean isValidDocument(String document) {
        return document.length() == 11 || document.length() == 14;
    }
}
