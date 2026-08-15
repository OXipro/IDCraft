package com.oxipro.idcraft.minestom.auth.prompt;

public final class PasswordChangeSubmission {

    private final String currentPassword;
    private final String newPassword;
    private final String confirmPassword;

    public PasswordChangeSubmission(String currentPassword, String newPassword, String confirmPassword) {
        this.currentPassword = currentPassword;
        this.newPassword = newPassword;
        this.confirmPassword = confirmPassword;
    }

    public String currentPassword() {
        return currentPassword;
    }

    public String newPassword() {
        return newPassword;
    }

    public String confirmPassword() {
        return confirmPassword;
    }
}
