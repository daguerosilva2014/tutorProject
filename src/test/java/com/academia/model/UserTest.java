package com.academia.model;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class UserTest {

    @Test
    void normalizesUsernameAndStartsEnabled() {
        User user = User.create("Ana", " Ana.Perez ", "hash", UserRole.TEACHER);

        assertEquals("ana.perez", user.username());
        assertEquals(UserStatus.ACTIVE, user.status());
    }

    @Test
    void canBeDisabledAndEnabled() {
        User user = User.create("Ana", "ana", "hash", UserRole.TEACHER);

        user.deactivate();
        assertEquals(UserStatus.INACTIVE, user.status());

        user.activate();
        assertEquals(UserStatus.ACTIVE, user.status());
    }
}
