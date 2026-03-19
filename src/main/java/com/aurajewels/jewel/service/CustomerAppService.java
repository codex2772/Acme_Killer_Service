/*
 * MIT License
 *
 * Copyright (c) 2026 AuraJewels (Raviraj Bhosale)
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */
package com.aurajewels.jewel.service;

import com.aurajewels.jewel.dto.customerapp.*;
import com.aurajewels.jewel.entity.*;
import com.aurajewels.jewel.repository.*;
import com.aurajewels.jewel.security.JwtUtil;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Service for the customer-facing mobile application.
 *
 * @author Raviraj Bhosale
 */
@Service
@RequiredArgsConstructor
public class CustomerAppService {

    private final CustomerRepository customerRepository;
    private final StoreRepository storeRepository;
    private final JewelryItemRepository jewelryItemRepository;
    private final CustomerWishlistRepository wishlistRepository;
    private final CustomerEnquiryRepository enquiryRepository;
    private final CategoryRepository categoryRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtil jwtUtil;

    // ======================== STORES (Public) ========================

    /** List all active stores across all organizations. */
    @Transactional(readOnly = true)
    public List<StoreListResponse> listAllStores() {
        return storeRepository.findByActiveTrueOrderByNameAsc().stream()
                .map(
                        store ->
                                StoreListResponse.builder()
                                        .id(store.getId())
                                        .name(store.getName())
                                        .organizationName(store.getOrganization().getName())
                                        .address(store.getAddress())
                                        .city(store.getCity())
                                        .state(store.getState())
                                        .pincode(store.getPincode())
                                        .phone(store.getPhone())
                                        .build())
                .toList();
    }

    /** Get store details with categories and catalog summary. */
    @Transactional(readOnly = true)
    public StoreDetailResponse getStoreDetail(Long storeId) {
        Store store =
                storeRepository
                        .findById(storeId)
                        .orElseThrow(() -> new IllegalArgumentException("Store not found"));

        List<Category> categories = categoryRepository.findByStoreIdAndActiveTrue(storeId);
        List<JewelryItem> items = jewelryItemRepository.findByStoreIdAndActiveTrue(storeId);

        List<StoreDetailResponse.CategoryInfo> categoryInfos =
                categories.stream()
                        .map(
                                cat ->
                                        StoreDetailResponse.CategoryInfo.builder()
                                                .id(cat.getId())
                                                .name(cat.getName())
                                                .description(cat.getDescription())
                                                .itemCount(
                                                        items.stream()
                                                                .filter(
                                                                        i ->
                                                                                i.getCategory()
                                                                                        .getId()
                                                                                        .equals(
                                                                                                cat
                                                                                                        .getId()))
                                                                .count())
                                                .build())
                        .toList();

        return StoreDetailResponse.builder()
                .id(store.getId())
                .name(store.getName())
                .organizationName(store.getOrganization().getName())
                .address(store.getAddress())
                .city(store.getCity())
                .state(store.getState())
                .pincode(store.getPincode())
                .phone(store.getPhone())
                .gstin(store.getGstin())
                .categories(categoryInfos)
                .totalItems(items.size())
                .build();
    }

    // ======================== AUTH ========================

    @Transactional
    public CustomerLoginResponse register(CustomerRegisterRequest request) {
        if (request.getPhone() == null || request.getPhone().isBlank()) {
            throw new IllegalArgumentException("Phone number is required");
        }
        if (request.getPassword() == null || request.getPassword().length() < 6) {
            throw new IllegalArgumentException("Password must be at least 6 characters");
        }
        if (request.getFirstName() == null || request.getFirstName().isBlank()) {
            throw new IllegalArgumentException("First name is required");
        }
        if (request.getStoreId() == null) {
            throw new IllegalArgumentException("Store ID is required");
        }

        Store store =
                storeRepository
                        .findById(request.getStoreId())
                        .orElseThrow(() -> new IllegalArgumentException("Store not found"));

        // Check if customer already exists for this store
        if (customerRepository
                .findByPhoneAndStoreId(request.getPhone(), request.getStoreId())
                .isPresent()) {
            throw new IllegalArgumentException(
                    "An account with this phone number already exists for this store");
        }

        Customer customer =
                Customer.builder()
                        .store(store)
                        .firstName(request.getFirstName())
                        .lastName(request.getLastName())
                        .phone(request.getPhone())
                        .email(request.getEmail())
                        .passwordHash(passwordEncoder.encode(request.getPassword()))
                        .build();
        customer.setActive(true);
        customer = customerRepository.save(customer);

        String token = jwtUtil.generateCustomerToken(customer, store.getId());
        String refreshToken = jwtUtil.generateCustomerRefreshToken(customer);

        return CustomerLoginResponse.builder()
                .token(token)
                .refreshToken(refreshToken)
                .customerId(customer.getId())
                .name(customer.getFirstName())
                .phone(customer.getPhone())
                .email(customer.getEmail())
                .storeId(store.getId())
                .storeName(store.getName())
                .build();
    }

    @Transactional(readOnly = true)
    public CustomerLoginResponse login(CustomerLoginRequest request) {
        if (request.getPhone() == null || request.getPassword() == null) {
            throw new IllegalArgumentException("Phone and password are required");
        }

        Customer customer =
                customerRepository
                        .findByPhoneAndActiveTrue(request.getPhone())
                        .orElseThrow(
                                () ->
                                        new IllegalArgumentException(
                                                "Invalid phone number or password"));

        if (customer.getPasswordHash() == null) {
            throw new IllegalArgumentException(
                    "Account not set up for mobile login. Please register first.");
        }

        if (!passwordEncoder.matches(request.getPassword(), customer.getPasswordHash())) {
            throw new IllegalArgumentException("Invalid phone number or password");
        }

        Long storeId =
                request.getStoreId() != null ? request.getStoreId() : customer.getStore().getId();

        Store store =
                storeRepository
                        .findById(storeId)
                        .orElseThrow(() -> new IllegalArgumentException("Store not found"));

        String token = jwtUtil.generateCustomerToken(customer, storeId);
        String refreshToken = jwtUtil.generateCustomerRefreshToken(customer);

        return CustomerLoginResponse.builder()
                .token(token)
                .refreshToken(refreshToken)
                .customerId(customer.getId())
                .name(customer.getFirstName())
                .phone(customer.getPhone())
                .email(customer.getEmail())
                .storeId(storeId)
                .storeName(store.getName())
                .build();
    }

    // ======================== CATALOG ========================

    @Transactional(readOnly = true)
    public List<CatalogItemResponse> getCatalog(Long storeId, Long customerId) {
        List<JewelryItem> items =
                jewelryItemRepository.findByStoreIdAndActiveTrue(storeId).stream()
                        .filter(i -> i.getStatus() == JewelryItem.ItemStatus.IN_STOCK)
                        .toList();

        Set<Long> wishlistedIds =
                customerId != null
                        ? wishlistRepository
                                .findByCustomerIdOrderByCreatedAtDesc(customerId)
                                .stream()
                                .map(w -> w.getJewelryItem().getId())
                                .collect(Collectors.toSet())
                        : Set.of();

        return items.stream().map(item -> toCatalogItem(item, wishlistedIds)).toList();
    }

    @Transactional(readOnly = true)
    public CatalogItemResponse getCatalogItem(Long storeId, Long itemId, Long customerId) {
        JewelryItem item =
                jewelryItemRepository
                        .findByIdAndStoreId(itemId, storeId)
                        .orElseThrow(() -> new IllegalArgumentException("Item not found"));

        boolean wishlisted =
                customerId != null
                        && wishlistRepository.existsByCustomerIdAndJewelryItemId(
                                customerId, itemId);

        return toCatalogItem(item, wishlisted ? Set.of(itemId) : Set.of());
    }

    // ======================== WISHLIST ========================

    @Transactional(readOnly = true)
    public List<CatalogItemResponse> getWishlist(Long customerId) {
        List<CustomerWishlist> wishlisted =
                wishlistRepository.findByCustomerIdOrderByCreatedAtDesc(customerId);

        return wishlisted.stream()
                .map(w -> toCatalogItem(w.getJewelryItem(), Set.of(w.getJewelryItem().getId())))
                .toList();
    }

    @Transactional
    public void addToWishlist(Long customerId, Long jewelryItemId) {
        if (wishlistRepository.existsByCustomerIdAndJewelryItemId(customerId, jewelryItemId)) {
            throw new IllegalArgumentException("Item already in wishlist");
        }

        Customer customer =
                customerRepository
                        .findById(customerId)
                        .orElseThrow(() -> new IllegalArgumentException("Customer not found"));

        JewelryItem item =
                jewelryItemRepository
                        .findById(jewelryItemId)
                        .orElseThrow(() -> new IllegalArgumentException("Item not found"));

        CustomerWishlist wishlist =
                CustomerWishlist.builder()
                        .customer(customer)
                        .jewelryItem(item)
                        .store(item.getStore())
                        .build();
        wishlistRepository.save(wishlist);
    }

    @Transactional
    public void removeFromWishlist(Long customerId, Long jewelryItemId) {
        CustomerWishlist wishlist =
                wishlistRepository
                        .findByCustomerIdAndJewelryItemId(customerId, jewelryItemId)
                        .orElseThrow(() -> new IllegalArgumentException("Item not in wishlist"));
        wishlistRepository.delete(wishlist);
    }

    // ======================== ENQUIRY ========================

    @Transactional
    public EnquiryResponse createEnquiry(Long customerId, EnquiryRequest request) {
        if (request.getSubject() == null || request.getSubject().isBlank()) {
            throw new IllegalArgumentException("Subject is required");
        }
        if (request.getMessage() == null || request.getMessage().isBlank()) {
            throw new IllegalArgumentException("Message is required");
        }

        Customer customer =
                customerRepository
                        .findById(customerId)
                        .orElseThrow(() -> new IllegalArgumentException("Customer not found"));

        JewelryItem item = null;
        if (request.getJewelryItemId() != null) {
            item =
                    jewelryItemRepository
                            .findById(request.getJewelryItemId())
                            .orElseThrow(() -> new IllegalArgumentException("Item not found"));
        }

        CustomerEnquiry enquiry =
                CustomerEnquiry.builder()
                        .customer(customer)
                        .jewelryItem(item)
                        .store(customer.getStore())
                        .subject(request.getSubject())
                        .message(request.getMessage())
                        .imageUrl(request.getImageUrl())
                        .status(CustomerEnquiry.EnquiryStatus.OPEN)
                        .build();
        enquiry = enquiryRepository.save(enquiry);

        return toEnquiryResponse(enquiry);
    }

    @Transactional(readOnly = true)
    public List<EnquiryResponse> getEnquiries(Long customerId) {
        return enquiryRepository.findByCustomerIdOrderByCreatedAtDesc(customerId).stream()
                .map(this::toEnquiryResponse)
                .toList();
    }

    // ======================== PROFILE ========================

    @Transactional(readOnly = true)
    public CustomerProfileResponse getProfile(Long customerId) {
        Customer customer =
                customerRepository
                        .findById(customerId)
                        .orElseThrow(() -> new IllegalArgumentException("Customer not found"));
        return toProfileResponse(customer);
    }

    @Transactional
    public CustomerProfileResponse updateProfile(
            Long customerId, CustomerProfileUpdateRequest request) {
        Customer customer =
                customerRepository
                        .findById(customerId)
                        .orElseThrow(() -> new IllegalArgumentException("Customer not found"));

        if (request.getFirstName() != null) customer.setFirstName(request.getFirstName());
        if (request.getLastName() != null) customer.setLastName(request.getLastName());
        if (request.getEmail() != null) customer.setEmail(request.getEmail());
        if (request.getAddressLine1() != null) customer.setAddressLine1(request.getAddressLine1());
        if (request.getAddressLine2() != null) customer.setAddressLine2(request.getAddressLine2());
        if (request.getCity() != null) customer.setCity(request.getCity());
        if (request.getState() != null) customer.setState(request.getState());
        if (request.getPincode() != null) customer.setPincode(request.getPincode());

        customerRepository.save(customer);
        return toProfileResponse(customer);
    }

    // ======================== MAPPERS ========================

    private CatalogItemResponse toCatalogItem(JewelryItem item, Set<Long> wishlistedIds) {
        return CatalogItemResponse.builder()
                .id(item.getId())
                .sku(item.getSku())
                .name(item.getName())
                .description(item.getDescription())
                .categoryName(item.getCategory() != null ? item.getCategory().getName() : null)
                .metalName(item.getMetalType() != null ? item.getMetalType().getName() : null)
                .purity(item.getMetalType() != null ? item.getMetalType().getPurity() : null)
                .grossWeight(item.getGrossWeight())
                .netWeight(item.getNetWeight())
                .makingCharges(item.getMakingCharges())
                .stoneCharges(item.getStoneCharges())
                .metalRate(
                        item.getMetalType() != null ? item.getMetalType().getCurrentRate() : null)
                .hsnCode(item.getHsnCode())
                .status(item.getStatus() != null ? item.getStatus().name() : null)
                .wishlisted(wishlistedIds.contains(item.getId()))
                .createdAt(item.getCreatedAt())
                .build();
    }

    private EnquiryResponse toEnquiryResponse(CustomerEnquiry enquiry) {
        return EnquiryResponse.builder()
                .id(enquiry.getId())
                .jewelryItemId(
                        enquiry.getJewelryItem() != null ? enquiry.getJewelryItem().getId() : null)
                .jewelryItemName(
                        enquiry.getJewelryItem() != null
                                ? enquiry.getJewelryItem().getName()
                                : null)
                .subject(enquiry.getSubject())
                .message(enquiry.getMessage())
                .imageUrl(enquiry.getImageUrl())
                .status(enquiry.getStatus().name())
                .adminResponse(enquiry.getAdminResponse())
                .createdAt(enquiry.getCreatedAt())
                .respondedAt(enquiry.getRespondedAt())
                .build();
    }

    private CustomerProfileResponse toProfileResponse(Customer customer) {
        return CustomerProfileResponse.builder()
                .id(customer.getId())
                .firstName(customer.getFirstName())
                .lastName(customer.getLastName())
                .phone(customer.getPhone())
                .email(customer.getEmail())
                .addressLine1(customer.getAddressLine1())
                .addressLine2(customer.getAddressLine2())
                .city(customer.getCity())
                .state(customer.getState())
                .pincode(customer.getPincode())
                .build();
    }
}
