package com.nidas.modules.account;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.nidas.infra.AbstractContainerBaseTest;
import com.nidas.infra.config.MockMvcTest;
import com.nidas.infra.config.TestProperties;
import com.nidas.infra.mail.EmailMessage;
import com.nidas.infra.mail.EmailService;
import com.nidas.modules.account.form.DetailsForm;
import com.nidas.modules.account.form.PasswordForm;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.times;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.response.SecurityMockMvcResultMatchers.authenticated;
import static org.springframework.security.test.web.servlet.response.SecurityMockMvcResultMatchers.unauthenticated;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@MockMvcTest
public class AccountControllerTest extends AbstractContainerBaseTest {

    @Autowired private MockMvc mockMvc;
    @Autowired private AccountRepository accountRepository;
    @Autowired private TestProperties testProperties;
    @Autowired private AccountFactory accountFactory;
    @Autowired private ObjectMapper objectMapper;
    @Autowired private PasswordEncoder passwordEncoder;

    @MockBean EmailService emailService;

    private final String TEST_EMAIL = "test@email.com";

    @DisplayName("???????????? ?????? ???????????? ?????????")
    @Test
    public void signUpForm() throws Exception {
        mockMvc.perform(get("/sign-up"))
                    .andDo(print())
                    .andExpect(status().isOk())
                    .andExpect(view().name("account/sign-up"))
                .andExpect(model().attributeExists("signUpForm"))
                .andExpect(unauthenticated());
    }

    @DisplayName("???????????? ?????? - ??????")
    @Test
    public void signUp() throws Exception {
        mockMvc.perform(post("/sign-up")
                    .param("email", testProperties.getEmail())
                    .param("password1", testProperties.getPassword())
                    .param("password2", testProperties.getPassword())
                    .param("name", testProperties.getName())
                    .param("gender", testProperties.getGender())
                    .param("birthday", testProperties.getBirthday())
                    .param("phoneNumber", testProperties.getPhoneNumber())
                    .with(csrf()))
                .andDo(print())
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/"))
                .andExpect(authenticated().withUsername(testProperties.getEmail()));

        Account account = accountRepository.findByEmail(testProperties.getEmail());
        assertNotNull(account);
        assertNotEquals(account.getPassword(), testProperties.getPassword()); // ???????????? ????????? ??????
        assertNotNull(account.getEmailCheckToken()); // ????????? ?????? ?????? ??????
        then(emailService).should(times(1)).sendEmail(any(EmailMessage.class)); // ????????? ???????????? ??????
    }

    @DisplayName("???????????? ?????? - ????????? ?????????(????????? ?????????)")
    @Test
    public void signUp_with_wrong_input_1() throws Exception {
        accountFactory.createAccount(testProperties.getEmail());
        mockMvc.perform(post("/sign-up")
                    .param("email", testProperties.getEmail())
                    .param("password1", testProperties.getPassword())
                    .param("password2", testProperties.getPassword())
                    .param("name", testProperties.getName())
                    .param("gender", testProperties.getGender())
                    .param("birthday", testProperties.getBirthday())
                    .param("phoneNumber", testProperties.getPhoneNumber())
                    .with(csrf()))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(view().name("account/sign-up"))
                .andExpect(model().hasErrors())
                .andExpect(unauthenticated());

        then(emailService).shouldHaveNoMoreInteractions(); // ???????????? ????????? ????????? ??????
    }

    @DisplayName("???????????? ?????? - ????????? ?????????(???????????? ??????)")
    @Test
    public void signUp_with_wrong_input_2() throws Exception {
        mockMvc.perform(post("/sign-up")
                    .param("email", testProperties.getEmail())
                    .param("password1", testProperties.getPassword())
                    .param("password2", "1234abcd!")
                    .param("name", testProperties.getName())
                    .param("gender", testProperties.getGender())
                    .param("birthday", testProperties.getBirthday())
                    .param("phoneNumber", testProperties.getPhoneNumber())
                    .with(csrf()))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(view().name("account/sign-up"))
                .andExpect(model().hasErrors())
                .andExpect(unauthenticated());

        assertNull(accountRepository.findByEmail(testProperties.getEmail()));
        then(emailService).shouldHaveNoMoreInteractions(); // ???????????? ????????? ????????? ??????
    }

    @DisplayName("???????????? ?????? - ????????? ?????????(????????? ?????????)")
    @Test
    public void signUp_with_wrong_input_3() throws Exception {
        mockMvc.perform(post("/sign-up")
                    .param("email", testProperties.getEmail())
                    .param("password1", testProperties.getPassword())
                    .param("password2", testProperties.getPassword())
                    .param("name", testProperties.getName())
                    .param("gender", "S")
                    .param("birthday", testProperties.getBirthday())
                    .param("phoneNumber", testProperties.getPhoneNumber())
                    .with(csrf()))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(view().name("account/sign-up"))
                .andExpect(model().hasErrors())
                .andExpect(unauthenticated());

        assertNull(accountRepository.findByEmail(testProperties.getEmail()));
        then(emailService).shouldHaveNoMoreInteractions(); // ???????????? ????????? ????????? ??????
    }

    @DisplayName("???????????? ?????? - ????????? ?????????(????????? ?????????)")
    @Test
    public void signUp_with_wrong_input_4() throws Exception {
        mockMvc.perform(post("/sign-up")
                    .param("email", testProperties.getEmail())
                    .param("password1", testProperties.getPassword())
                    .param("password2", testProperties.getPassword())
                    .param("name", testProperties.getName())
                    .param("gender", testProperties.getGender())
                    .param("birthday", "19800132")
                    .param("phoneNumber", testProperties.getPhoneNumber())
                    .with(csrf()))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(view().name("account/sign-up"))
                .andExpect(model().hasErrors())
                .andExpect(unauthenticated());

        assertNull(accountRepository.findByEmail(testProperties.getEmail()));
        then(emailService).shouldHaveNoMoreInteractions(); // ???????????? ????????? ????????? ??????
    }

    @Test
    @DisplayName("????????? ?????? ?????? ?????? - ??????")
    void checkEmailToken() throws Exception {
        Account account = accountFactory.createAccount(testProperties.getEmail());
        mockMvc.perform(get("/check-email-token")
                    .param("email", account.getEmail())
                    .param("token", account.getEmailCheckToken()))
                .andExpect(status().isOk())
                .andExpect(view().name("account/checked-email"))
                .andExpect(model().attributeExists("successMessage"))
                .andExpect(authenticated().withUsername(account.getEmail()));

        assertTrue(account.isEmailVerified()); // ????????? ????????? ???????????? ??????
    }

    @Test
    @DisplayName("????????? ?????? ?????? - ????????? ?????????(???????????? ?????? ?????????)")
    void validateEmailToken_with_wrong_value_1() throws Exception {
        mockMvc.perform(get("/check-email-token")
                    .param("email", testProperties.getEmail())
                    .param("token", "token123"))
                .andExpect(status().isOk())
                .andExpect(view().name("account/checked-email"))
                .andExpect(model().attributeExists("errorMessage"))
                .andExpect(unauthenticated());
    }

    @Test
    @DisplayName("????????? ?????? ?????? - ????????? ?????????(???????????? ?????? ??????)")
    void validateEmailToken_with_wrong_value_2() throws Exception {
        Account account = accountFactory.createAccount(testProperties.getEmail());
        mockMvc.perform(get("/check-email-token")
                    .param("email", account.getEmail())
                    .param("token", account.getEmailCheckToken() + "diff"))
                .andExpect(status().isOk())
                .andExpect(view().name("account/checked-email"))
                .andExpect(model().attributeExists("errorMessage"))
                .andExpect(unauthenticated());
    }

    @DisplayName("????????? ?????? ???????????? ?????????")
    @Test
    public void login() throws Exception {
        mockMvc.perform(get("/login"))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(view().name("account/login"))
                .andExpect(unauthenticated());
    }

    @DisplayName("???????????? ?????? ?????? ???????????? ?????????")
    @Test
    public void findPasswordForm() throws Exception {
        mockMvc.perform(get("/find-password"))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(view().name("account/find-password"))
                .andExpect(unauthenticated());
    }

    @Test
    @DisplayName("???????????? ?????? ?????? - ??????")
    void findPassword() throws Exception {
        Account account = accountFactory.createAccount(testProperties.getEmail());
        mockMvc.perform(post("/find-password")
                    .param("email", account.getEmail())
                    .with(csrf()))
                .andDo(print())
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/find-password"))
                .andExpect(flash().attributeExists("successMessage"))
                .andExpect(unauthenticated());

        assertNotNull(account.getPasswordFindToken()); // ???????????? ????????? ????????? ?????? ?????? ??????
        then(emailService).should(times(1)).sendEmail(any(EmailMessage.class)); // ????????? ???????????? ??????
    }

    @Test
    @DisplayName("???????????? ?????? ?????? - ????????? ?????????(???????????? ?????? ?????????)")
    void findPassword_with_wrong_value_1() throws Exception {
        mockMvc.perform(post("/find-password")
                    .param("email", testProperties.getEmail())
                    .with(csrf()))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(view().name("account/find-password"))
                .andExpect(model().attributeExists("errorMessage"))
                .andExpect(unauthenticated());

        then(emailService).shouldHaveNoMoreInteractions(); // ???????????? ????????? ????????? ??????
    }

    @Test
    @DisplayName("???????????? ?????? ?????? - ????????? ?????????(????????? ?????????)")
    void findPassword_with_wrong_value_2() throws Exception {
        Account account = accountFactory.createAccount(testProperties.getEmail());
        account.generatePasswordFindToken(); // ?????? ??? ???????????? ????????? ?????? ??????

        mockMvc.perform(post("/find-password")
                    .param("email", account.getEmail())
                    .with(csrf()))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(view().name("account/find-password"))
                .andExpect(model().attributeExists("errorMessage"))
                .andExpect(unauthenticated());

        then(emailService).shouldHaveNoMoreInteractions(); // ???????????? ????????? ????????? ??????
    }

    @Test
    @DisplayName("???????????? ?????? ?????? ?????? - ??????")
    void checkPasswordFindToken() throws Exception {
        Account account = accountFactory.createAccount(testProperties.getEmail());
        account.generatePasswordFindToken();

        mockMvc.perform(get("/check-password-find-token")
                    .param("email", account.getEmail())
                    .param("token", account.getPasswordFindToken()))
                .andExpect(status().isOk())
                .andExpect(view().name("account/checked-email"))
                .andExpect(model().attributeExists("successMessage"))
                .andExpect(authenticated().withUsername(account.getEmail())); // ????????? ????????? ??????

        assertNull(account.getPasswordFindToken()); // ?????? ??? ???????????? ????????? ????????? ??????
    }

    @Test
    @DisplayName("???????????? ?????? ?????? ?????? - ????????? ?????????(???????????? ?????? ?????????)")
    void checkPasswordFindToken_with_wrong_value_1() throws Exception {
        mockMvc.perform(get("/check-password-find-token")
                    .param("email", testProperties.getEmail())
                    .param("token", "token123"))
                .andExpect(status().isOk())
                .andExpect(view().name("account/checked-email"))
                .andExpect(model().attributeExists("errorMessage"))
                .andExpect(unauthenticated());
    }

    @Test
    @DisplayName("???????????? ?????? ?????? ?????? - ????????? ?????????(???????????? ?????? ??????)")
    void checkPasswordFindToken_with_wrong_value_2() throws Exception {
        Account account = accountFactory.createAccount(testProperties.getEmail());
        account.generatePasswordFindToken();

        mockMvc.perform(get("/check-password-find-token")
                    .param("email", account.getEmail())
                    .param("token", account.getPasswordFindToken() + "diff"))
                .andExpect(status().isOk())
                .andExpect(view().name("account/checked-email"))
                .andExpect(model().attributeExists("errorMessage"))
                .andExpect(unauthenticated());
    }

    @WithAccount(TEST_EMAIL)
    @DisplayName("?????? ?????? ?????? ???????????? ?????????")
    @Test
    public void editInfoForm() throws Exception {
        mockMvc.perform(get("/mypage/edit/info"))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(view().name("account/edit-info"))
                .andExpect(model().attributeExists("account", "detailsForm", "passwordForm"))
                .andExpect(authenticated().withUsername(TEST_EMAIL));
    }

    @WithAccount(TEST_EMAIL)
    @Test
    @DisplayName("????????? ????????? ?????? - ??????")
    void resendVerificationCode() throws Exception {
        Account account = accountRepository.findByEmail(TEST_EMAIL);
        account.setEmailCheckTokenGeneratedDateTime(LocalDateTime.now().minusHours(2));
        account.setEmailVerified(false);

        mockMvc.perform(post("/resend/verification-code")
                .with(csrf()))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(authenticated().withUsername(TEST_EMAIL));

        then(emailService).should(times(2)).sendEmail(any(EmailMessage.class)); // ????????? ???????????? ??????
    }

    @WithAccount(TEST_EMAIL)
    @Test
    @DisplayName("????????? ????????? ?????? - ????????? ?????????(?????? ????????? ??????)")
    void resendVerificationCode_with_wrong_value_1() throws Exception {
        Account account = accountRepository.findByEmail(TEST_EMAIL);
        account.setEmailCheckTokenGeneratedDateTime(LocalDateTime.now().minusHours(2));
        account.setEmailVerified(true);

        String content = mockMvc.perform(post("/resend/verification-code")
                    .with(csrf()))
                .andDo(print())
                .andExpect(status().isBadRequest())
                .andExpect(authenticated().withUsername(TEST_EMAIL)).andReturn().getResponse().getContentAsString();

        assertTrue(content.contains("message"));
        then(emailService).should(times(1)).sendEmail(any(EmailMessage.class)); // ???????????? ????????? ????????? ??????
    }

    @WithAccount(TEST_EMAIL)
    @Test
    @DisplayName("????????? ????????? ?????? - ????????? ?????????(????????? ?????????)")
    void resendVerificationCode_with_wrong_value_2() throws Exception {
        // test security context ?????? ?????? ???????????? ????????? ???????????? ????????? ????????? ??????
        String content = mockMvc.perform(post("/resend/verification-code")
                    .with(csrf()))
                .andDo(print())
                .andExpect(status().isBadRequest())
                .andExpect(authenticated().withUsername(TEST_EMAIL)).andReturn().getResponse().getContentAsString();

        assertTrue(content.contains("message"));
        then(emailService).should(times(1)).sendEmail(any(EmailMessage.class)); // ???????????? ????????? ????????? ??????
    }

    @WithAccount(TEST_EMAIL)
    @Test
    @DisplayName("?????? ?????? ?????? ?????? - ??????")
    void editDetailsInfo() throws Exception {
        DetailsForm detailsForm = new DetailsForm();
        detailsForm.setName("?????????");
        detailsForm.setGender(Gender.U);
        detailsForm.setBirthday(LocalDate.parse("1990-03-31", DateTimeFormatter.ofPattern("yyyy-MM-dd")));
        detailsForm.setPhoneNumber("01087654321");

        String requestBody = objectMapper.writeValueAsString(detailsForm);
        mockMvc.perform(post("/mypage/edit/info/details")
                    .characterEncoding("utf8")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(requestBody)
                    .with(csrf()))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(authenticated().withUsername(TEST_EMAIL));

        Account account = accountRepository.findByEmail(TEST_EMAIL);
        assertEquals(account.getName(), detailsForm.getName());
        assertEquals(account.getGender(), detailsForm.getGender());
        assertEquals(account.getBirthday(), detailsForm.getBirthday());
        assertEquals(account.getPhoneNumber(), detailsForm.getPhoneNumber());
    }

    @WithAccount(TEST_EMAIL)
    @Test
    @DisplayName("?????? ?????? ?????? ?????? - ????????? ?????????")
    void editDetailsInfo_with_wrong_value() throws Exception {
        DetailsForm detailsForm = new DetailsForm();
        detailsForm.setName("123");
        detailsForm.setGender(null);
        detailsForm.setBirthday(LocalDate.parse("1800-03-31", DateTimeFormatter.ofPattern("yyyy-MM-dd")));
        detailsForm.setPhoneNumber("12345");

        String requestBody = objectMapper.writeValueAsString(detailsForm);
        mockMvc.perform(post("/mypage/edit/info/details")
                    .characterEncoding("utf8")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(requestBody)
                    .with(csrf()))
                .andDo(print())
                .andExpect(status().isBadRequest())
                .andExpect(authenticated().withUsername(TEST_EMAIL));
    }

    @WithAccount(TEST_EMAIL)
    @Test
    @DisplayName("????????? ?????? ?????? ?????? - ??????")
    void editPasswordsInfo() throws Exception {
        PasswordForm passwordForm = new PasswordForm();
        passwordForm.setCurrentPassword(testProperties.getPassword());
        passwordForm.setNewPassword1(testProperties.getPassword() + "new");
        passwordForm.setNewPassword2(testProperties.getPassword() + "new");

        String requestBody = objectMapper.writeValueAsString(passwordForm);
        mockMvc.perform(post("/mypage/edit/info/password")
                    .characterEncoding("utf8")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(requestBody)
                    .with(csrf()))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(authenticated().withUsername(TEST_EMAIL));

        Account account = accountRepository.findByEmail(TEST_EMAIL);
        assertTrue(passwordEncoder.matches(passwordForm.getNewPassword1(), account.getPassword()));
    }

    @WithAccount(TEST_EMAIL)
    @Test
    @DisplayName("????????? ?????? ?????? ?????? - ???????????? ??????")
    void editPasswordsInfo_with_wrong_value_1() throws Exception {
        PasswordForm passwordForm = new PasswordForm();
        passwordForm.setCurrentPassword(testProperties.getPassword() + "wrong");
        passwordForm.setNewPassword1(testProperties.getPassword() + "new");
        passwordForm.setNewPassword2(testProperties.getPassword() + "new");

        String requestBody = objectMapper.writeValueAsString(passwordForm);
        mockMvc.perform(post("/mypage/edit/info/password")
                    .characterEncoding("utf8")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(requestBody)
                    .with(csrf()))
                .andDo(print())
                .andExpect(status().isBadRequest())
                .andExpect(authenticated().withUsername(TEST_EMAIL));

        Account account = accountRepository.findByEmail(TEST_EMAIL);
        assertFalse(passwordEncoder.matches(passwordForm.getNewPassword1(), account.getPassword()));
    }

    @WithAccount(TEST_EMAIL)
    @Test
    @DisplayName("????????? ?????? ?????? ?????? - ??? ???????????? ?????? ??????")
    void editPasswordsInfo_with_wrong_value_2() throws Exception {
        PasswordForm passwordForm = new PasswordForm();
        passwordForm.setCurrentPassword(testProperties.getPassword());
        passwordForm.setNewPassword1(testProperties.getPassword() + "new1");
        passwordForm.setNewPassword2(testProperties.getPassword() + "new2");

        String requestBody = objectMapper.writeValueAsString(passwordForm);
        mockMvc.perform(post("/mypage/edit/info/password")
                    .characterEncoding("utf8")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(requestBody)
                    .with(csrf()))
                .andDo(print())
                .andExpect(status().isBadRequest())
                .andExpect(authenticated().withUsername(TEST_EMAIL));

        Account account = accountRepository.findByEmail(TEST_EMAIL);
        assertFalse(passwordEncoder.matches(passwordForm.getNewPassword1(), account.getPassword()));
    }

    @WithAccount(TEST_EMAIL)
    @Test
    @DisplayName("??? ?????? ?????? ?????? ?????? ??????")
    void editWebReceptionInfo() throws Exception {
        Account account = accountRepository.findByEmail(TEST_EMAIL);
        boolean prev = account.isReceivingInfoByWeb();

        mockMvc.perform(post("/mypage/edit/info/web-reception")
                    .with(csrf()))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(authenticated().withUsername(TEST_EMAIL));

        assertEquals(!prev, account.isReceivingInfoByWeb());
    }

    @WithAccount(TEST_EMAIL)
    @Test
    @DisplayName("????????? ?????? ?????? ?????? ?????? ??????")
    void editEmailReceptionInfo() throws Exception {
        Account account = accountRepository.findByEmail(TEST_EMAIL);
        boolean prev = account.isReceivingInfoByEmail();

        mockMvc.perform(post("/mypage/edit/info/email-reception")
                    .with(csrf()))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(authenticated().withUsername(TEST_EMAIL));

        assertEquals(!prev, account.isReceivingInfoByEmail());
    }

    @WithAccount(TEST_EMAIL)
    @Test
    @DisplayName("?????? ?????? ??????")
    void signOut() throws Exception {
        mockMvc.perform(post("/sign-out")
                    .with(csrf()))
                .andDo(print())
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/"))
                .andExpect(unauthenticated()); // ???????????????

        assertTrue(accountRepository.findByEmail(TEST_EMAIL).isDeleted());
    }

    @WithAccount(TEST_EMAIL)
    @DisplayName("???????????? ?????? ?????? ???????????? ?????????")
    @Test
    public void mileageForm() throws Exception {
        mockMvc.perform(get("/mypage/mileage"))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(view().name("account/mileage"))
                .andExpect(model().attributeExists("account"))
                .andExpect(authenticated().withUsername(TEST_EMAIL));
    }

    @WithAccount(TEST_EMAIL)
    @DisplayName("????????? ?????? ?????? ???????????? ?????????")
    @Test
    public void membershipForm() throws Exception {
        mockMvc.perform(get("/mypage/membership"))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(view().name("account/membership"))
                .andExpect(model().attributeExists("account"))
                .andExpect(authenticated().withUsername(TEST_EMAIL));
    }

}
