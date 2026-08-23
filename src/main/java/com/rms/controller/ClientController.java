package com.rms.controller;

import com.rms.dto.ClientRequest;
import com.rms.dto.ClientResponse;
import com.rms.service.ClientService;
import jakarta.validation.Valid;
import java.util.List;
import java.util.UUID;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.util.UriComponentsBuilder;
import io.swagger.v3.oas.annotations.tags.Tag;

@RestController
@RequestMapping("/clients")
@Tag(name = "Clients", description = "Cadastro e manutenÃƒÂ§ÃƒÂ£o de clientes")
public class ClientController {
    private final ClientService service;

    public ClientController(ClientService service) { this.service = service; }

    @PostMapping
    public ResponseEntity<ClientResponse> create(@Valid @RequestBody ClientRequest request, UriComponentsBuilder uri) {
        ClientResponse response = service.create(request);
        return ResponseEntity.created(uri.path("/clients/{id}").build(response.id())).body(response);
    }

    @GetMapping
    public List<ClientResponse> list() { return service.list(); }

    @GetMapping("/archived")
    public List<ClientResponse> archived() { return service.listArchived(); }

    @GetMapping("/{id}")
    public ClientResponse get(@PathVariable UUID id) { return ClientResponse.from(service.get(id)); }

    @PutMapping("/{id}")
    public ClientResponse update(@PathVariable UUID id, @Valid @RequestBody ClientRequest request) {
        return service.update(id, request);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable UUID id) {
        service.delete(id);
        return ResponseEntity.noContent().build();
    }
}
