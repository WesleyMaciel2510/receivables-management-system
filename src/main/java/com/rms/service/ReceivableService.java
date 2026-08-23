package com.rms.service;

import com.rms.dto.ReceivableRequest;
import com.rms.dto.ReceivableResponse;
import com.rms.model.Client;
import com.rms.model.Receivable;
import com.rms.model.ReceivableStatus;
import com.rms.repository.ClientRepository;
import com.rms.repository.ReceivableRepository;
import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class ReceivableService {
    private static final Map<ReceivableStatus, Set<ReceivableStatus>> ALLOWED_TRANSITIONS = Map.of(
            ReceivableStatus.PENDING, Set.of(ReceivableStatus.PARTIAL, ReceivableStatus.PAID,
                    ReceivableStatus.OVERDUE, ReceivableStatus.CANCELLED),
            ReceivableStatus.PARTIAL, Set.of(ReceivableStatus.PAID, ReceivableStatus.OVERDUE, ReceivableStatus.CANCELLED),
            ReceivableStatus.OVERDUE, Set.of(ReceivableStatus.PAID, ReceivableStatus.CANCELLED),
            ReceivableStatus.PAID, Set.of(),
            ReceivableStatus.CANCELLED, Set.of());

    private final ReceivableRepository receivableRepository;
    private final ClientRepository clientRepository;

    public ReceivableService(ReceivableRepository receivableRepository, ClientRepository clientRepository) {
        this.receivableRepository = receivableRepository;
        this.clientRepository = clientRepository;
    }

    public ReceivableResponse create(ReceivableRequest request) {
        Client client = clientRepository.findById(request.clientId())
                .orElseThrow(() -> new ResourceNotFoundException("Client not found: " + request.clientId()));
        return ReceivableResponse.from(receivableRepository.save(new Receivable(client, request.description().trim(),
                request.amount(), request.dueDate())));
    }

    @Transactional(readOnly = true)
    public List<ReceivableResponse> list() {
        return receivableRepository.findAll().stream().map(ReceivableResponse::from).toList();
    }

    @Transactional(readOnly = true)
    public List<ReceivableResponse> listArchived() {
        return receivableRepository.findArchived().stream().map(ReceivableResponse::from).toList();
    }

    @Transactional(readOnly = true)
    public ReceivableResponse get(UUID id) {
        return ReceivableResponse.from(getEntity(id));
    }

    private Receivable getEntity(UUID id) {
        return receivableRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Receivable not found: " + id));
    }

    public ReceivableResponse update(UUID id, ReceivableRequest request) {
        Receivable receivable = getEntity(id);
        if (!receivable.getClient().getId().equals(request.clientId())) {
            throw new InvalidStateTransitionException("Receivable client cannot be changed");
        }
        BigDecimal difference = request.amount().subtract(receivable.getOriginalAmount());
        BigDecimal newBalance = receivable.getRemainingBalance().add(difference);
        if (newBalance.signum() < 0 || newBalance.compareTo(request.amount()) > 0) {
            throw new InvalidStateTransitionException("Update would violate remaining balance bounds");
        }
        receivable.update(request.description().trim(), request.amount(), newBalance, request.dueDate());
        return ReceivableResponse.from(receivableRepository.save(receivable));
    }

    public ReceivableResponse changeStatus(UUID id, ReceivableStatus status) {
        Receivable receivable = getEntity(id);
        if (!ALLOWED_TRANSITIONS.get(receivable.getStatus()).contains(status)) {
            throw new InvalidStateTransitionException("Cannot change status from " + receivable.getStatus() + " to " + status);
        }
        receivable.changeStatus(status);
        return ReceivableResponse.from(receivableRepository.save(receivable));
    }

    public void delete(UUID id) {
        receivableRepository.delete(getEntity(id));
    }
}
