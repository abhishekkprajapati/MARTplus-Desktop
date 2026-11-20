package com.martplus.desktop;

import com.martplus.desktop.dao.UserDao;
import com.martplus.desktop.exception.ValidationException;
import com.martplus.desktop.model.User;
import com.martplus.desktop.service.AuthenticationService;
import com.martplus.desktop.util.PasswordUtil;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@DisplayName("User Registration & Sign-Up Business Rules Tests")
public class UserRegistrationTest {

    private UserDao userDao;
    private AuthenticationService authService;

    @BeforeEach
    void setUp() {
        userDao = Mockito.mock(UserDao.class);
        authService = new AuthenticationService(userDao);
    }

    @Test
    @DisplayName("1. Successful Buyer registration saves user with BCrypt hash, ACTIVE status and verified flags")
    void testSuccessfulBuyerRegistration() {
        when(userDao.findByEmailAndRole("buyer@example.com", "BUYER")).thenReturn(Optional.empty());
        when(userDao.findByPhoneAndRole("9876543210", "BUYER")).thenReturn(Optional.empty());

        when(userDao.save(any(User.class))).thenAnswer(invocation -> {
            User u = invocation.getArgument(0);
            u.setId(101L);
            return u;
        });

        User registered = authService.register(
                "Jane", "Doe", "buyer@example.com", "9876543210",
                "Password@123", "Password@123", "BUYER"
        );

        assertNotNull(registered);
        assertEquals(101L, registered.getId());
        assertEquals("Jane", registered.getFirstName());
        assertEquals("Doe", registered.getLastName());
        assertEquals("buyer@example.com", registered.getEmail());
        assertEquals("9876543210", registered.getPhone());
        assertEquals("BUYER", registered.getRole());
        assertEquals("ACTIVE", registered.getStatus());
        assertTrue(registered.isEmailVerified());
        assertTrue(registered.isPhoneVerified());

        // Verify password was hashed with BCrypt
        assertTrue(PasswordUtil.checkPassword("Password@123", registered.getPassword()));

        ArgumentCaptor<User> captor = ArgumentCaptor.forClass(User.class);
        verify(userDao, times(1)).save(captor.capture());
        User savedUser = captor.getValue();
        assertEquals("buyer@example.com", savedUser.getEmail());
    }

    @Test
    @DisplayName("2. Successful Seller registration saves user as SELLER")
    void testSuccessfulSellerRegistration() {
        when(userDao.findByEmailAndRole("seller@store.com", "SELLER")).thenReturn(Optional.empty());
        when(userDao.findByPhoneAndRole("9123456780", "SELLER")).thenReturn(Optional.empty());

        when(userDao.save(any(User.class))).thenAnswer(invocation -> {
            User u = invocation.getArgument(0);
            u.setId(202L);
            return u;
        });

        User registered = authService.register(
                "Shop", "Owner", "seller@store.com", "9123456780",
                "SellerSecure88", "SellerSecure88", "SELLER"
        );

        assertNotNull(registered);
        assertEquals(202L, registered.getId());
        assertEquals("SELLER", registered.getRole());
        assertTrue(registered.isActive());
        assertTrue(PasswordUtil.checkPassword("SellerSecure88", registered.getPassword()));
        verify(userDao, times(1)).save(any(User.class));
    }

    @Test
    @DisplayName("3. Duplicate email + role rejection")
    void testDuplicateEmailAndRoleRejection() {
        User existing = new User(1L, "Existing", "User", "dup@martplus.com", "9999999999",
                "hashed", "BUYER", "ACTIVE", true, true, null);

        when(userDao.findByEmailAndRole("dup@martplus.com", "BUYER")).thenReturn(Optional.of(existing));

        ValidationException ex = assertThrows(ValidationException.class, () -> authService.register(
                "John", "Smith", "dup@martplus.com", "9876543210",
                "Pass12345", "Pass12345", "BUYER"
        ));

        assertTrue(ex.getMessage().contains("already registered as BUYER"));
        verify(userDao, never()).save(any(User.class));
    }

    @Test
    @DisplayName("4. Invalid email format rejection")
    void testInvalidEmailRejection() {
        ValidationException ex1 = assertThrows(ValidationException.class, () -> authService.register(
                "John", "Doe", "not-an-email", "9876543210",
                "Pass12345", "Pass12345", "BUYER"
        ));
        assertTrue(ex1.getMessage().contains("Invalid email address format"));

        ValidationException ex2 = assertThrows(ValidationException.class, () -> authService.register(
                "John", "Doe", "user@", "9876543210",
                "Pass12345", "Pass12345", "BUYER"
        ));
        assertTrue(ex2.getMessage().contains("Invalid email address format"));

        verify(userDao, never()).save(any());
    }

    @Test
    @DisplayName("5. Empty required fields rejection")
    void testEmptyRequiredFieldsRejection() {
        // Empty First Name
        ValidationException ex1 = assertThrows(ValidationException.class, () -> authService.register(
                "", "Doe", "valid@mail.com", "9876543210", "Pass12345", "Pass12345", "BUYER"
        ));
        assertTrue(ex1.getMessage().contains("First name is required"));

        // Empty Last Name
        ValidationException ex2 = assertThrows(ValidationException.class, () -> authService.register(
                "John", "   ", "valid@mail.com", "9876543210", "Pass12345", "Pass12345", "BUYER"
        ));
        assertTrue(ex2.getMessage().contains("Last name is required"));

        // Empty Email
        ValidationException ex3 = assertThrows(ValidationException.class, () -> authService.register(
                "John", "Doe", "", "9876543210", "Pass12345", "Pass12345", "BUYER"
        ));
        assertTrue(ex3.getMessage().contains("Email address is required"));

        // Empty Phone
        ValidationException ex4 = assertThrows(ValidationException.class, () -> authService.register(
                "John", "Doe", "valid@mail.com", "", "Pass12345", "Pass12345", "BUYER"
        ));
        assertTrue(ex4.getMessage().contains("Phone number is required"));

        // Empty Password
        ValidationException ex5 = assertThrows(ValidationException.class, () -> authService.register(
                "John", "Doe", "valid@mail.com", "9876543210", "", "Pass12345", "BUYER"
        ));
        assertTrue(ex5.getMessage().contains("Password is required"));

        verify(userDao, never()).save(any());
    }

    @Test
    @DisplayName("6. Invalid phone number rejection (must contain exactly 10 digits)")
    void testInvalidPhoneRejection() {
        // Less than 10 digits
        ValidationException ex1 = assertThrows(ValidationException.class, () -> authService.register(
                "John", "Doe", "valid@mail.com", "98765", "Pass12345", "Pass12345", "BUYER"
        ));
        assertTrue(ex1.getMessage().contains("Phone number must be exactly 10 digits"));

        // More than 10 digits
        ValidationException ex2 = assertThrows(ValidationException.class, () -> authService.register(
                "John", "Doe", "valid@mail.com", "9876543210123", "Pass12345", "Pass12345", "BUYER"
        ));
        assertTrue(ex2.getMessage().contains("Phone number must be exactly 10 digits"));

        // Letters in phone
        ValidationException ex3 = assertThrows(ValidationException.class, () -> authService.register(
                "John", "Doe", "valid@mail.com", "98765ABC10", "Pass12345", "Pass12345", "BUYER"
        ));
        assertTrue(ex3.getMessage().contains("Phone number must be exactly 10 digits"));

        verify(userDao, never()).save(any());
    }

    @Test
    @DisplayName("7. Password mismatch and length rejection")
    void testPasswordValidationRejection() {
        // Password too short (< 8 chars)
        ValidationException exShort = assertThrows(ValidationException.class, () -> authService.register(
                "John", "Doe", "valid@mail.com", "9876543210", "short", "short", "BUYER"
        ));
        assertTrue(exShort.getMessage().contains("Password must be at least 8 characters"));

        // Password mismatch
        ValidationException exMismatch = assertThrows(ValidationException.class, () -> authService.register(
                "John", "Doe", "valid@mail.com", "9876543210", "Password123", "Password999", "BUYER"
        ));
        assertTrue(exMismatch.getMessage().contains("Password and Confirm Password do not match"));

        verify(userDao, never()).save(any());
    }

    @Test
    @DisplayName("8. Password hashing verifies BCrypt compatibility")
    void testPasswordHashingCompatibility() {
        when(userDao.findByEmailAndRole(anyString(), anyString())).thenReturn(Optional.empty());
        when(userDao.findByPhoneAndRole(anyString(), anyString())).thenReturn(Optional.empty());
        when(userDao.save(any(User.class))).thenAnswer(i -> i.getArgument(0));

        String rawSecret = "SuperSecret#2026";
        User user = authService.register(
                "Alice", "Wonder", "alice@example.com", "9876543210",
                rawSecret, rawSecret, "BUYER"
        );

        assertNotEquals(rawSecret, user.getPassword());
        assertTrue(user.getPassword().startsWith("$2a$") || user.getPassword().startsWith("$2b$"));
        assertTrue(PasswordUtil.checkPassword(rawSecret, user.getPassword()));
        assertFalse(PasswordUtil.checkPassword("WrongSecret", user.getPassword()));
    }

    @Test
    @DisplayName("9. ADMIN self-registration rejection")
    void testAdminSelfRegistrationRejection() {
        ValidationException ex = assertThrows(ValidationException.class, () -> authService.register(
                "Malicious", "Actor", "admin@fake.com", "9876543210",
                "AdminPassword123", "AdminPassword123", "ADMIN"
        ));

        assertTrue(ex.getMessage().contains("Self-registration as ADMIN is strictly prohibited"));
        verify(userDao, never()).save(any());
    }
}

