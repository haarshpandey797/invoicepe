package com.invoicepe.service;

import com.invoicepe.dto.request.ClientRequest;
import com.invoicepe.dto.response.ClientResponse;
import com.invoicepe.exception.ResourceNotFoundException;
import com.invoicepe.model.Client;
import com.invoicepe.model.User;
import com.invoicepe.repository.ClientRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class ClientService {

    private final ClientRepository clientRepository;

    @Transactional(readOnly = true)
    public List<ClientResponse> listClients(User user) {
        return clientRepository.findByUserIdOrderByNameAsc(user.getId())
                .stream()
                .map(ClientResponse::from)
                .toList();
    }

    @Transactional
    public ClientResponse createClient(User user, ClientRequest request) {
        Client client = Client.builder()
                .user(user)
                .name(request.getName())
                .email(request.getEmail())
                .phone(request.getPhone())
                .gstin(request.getGstin())
                .billingAddress(request.getBillingAddress())
                .build();

        client = clientRepository.save(client);
        log.info("Client created: {} for user: {}", client.getId(), user.getId());
        return ClientResponse.from(client);
    }

    @Transactional
    public ClientResponse updateClient(User user, UUID clientId, ClientRequest request) {
        Client client = clientRepository.findByIdAndUserId(clientId, user.getId())
                .orElseThrow(() -> new ResourceNotFoundException("Client", clientId.toString()));

        client.setName(request.getName());
        client.setEmail(request.getEmail());
        client.setPhone(request.getPhone());
        client.setGstin(request.getGstin());
        client.setBillingAddress(request.getBillingAddress());

        return ClientResponse.from(clientRepository.save(client));
    }

    @Transactional
    public void deleteClient(User user, UUID clientId) {
        if (!clientRepository.existsByIdAndUserId(clientId, user.getId())) {
            throw new ResourceNotFoundException("Client", clientId.toString());
        }
        clientRepository.deleteById(clientId);
        log.info("Client deleted: {} by user: {}", clientId, user.getId());
    }
}
