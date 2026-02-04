package br.com.consisa.gov.kb.security;

import br.com.consisa.gov.kb.domain.AppUser;
import br.com.consisa.gov.kb.repository.AppUserRepository;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.jsonwebtoken.Claims;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class AuthIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private AppUserRepository userRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private JwtService jwtService;

    @Autowired
    private ObjectMapper objectMapper;

    private AppUser adminUser;
    private AppUser analystUser;

    @BeforeEach
    void setup() {
        userRepository.deleteAll();

        adminUser = new AppUser();
        adminUser.setEmail("admin@consisa.com");
        adminUser.setPasswordHash(passwordEncoder.encode("secret123"));
        adminUser.setRoles(Set.of("ADMIN"));
        userRepository.save(adminUser);

        analystUser = new AppUser();
        analystUser.setEmail("analyst@consisa.com");
        analystUser.setPasswordHash(passwordEncoder.encode("secret123"));
        analystUser.setRoles(Set.of("ANALYST"));
        userRepository.save(analystUser);
    }

    @Test
    void loginGeneratesJwtWithClaims() throws Exception {
        String payload = """
                {
                  "email": "admin@consisa.com",
                  "password": "secret123"
                }
                """;

        String response = mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(payload))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();

        Map<String, Object> body = objectMapper.readValue(response, new TypeReference<>() {});
        String accessToken = (String) body.get("accessToken");

        Claims claims = jwtService.parseClaims(accessToken);
        assertThat(claims.getSubject()).isEqualTo(String.valueOf(adminUser.getId()));
        assertThat(claims.get("email", String.class)).isEqualTo(adminUser.getEmail());
        assertThat(claims.get("roles", List.class)).contains("ADMIN");
    }

    @Test
    void protectedEndpointReturns401WithoutToken() throws Exception {
        mockMvc.perform(get("/api/v1/dashboard/summary"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void endpointReturns403WithoutRole() throws Exception {
        String token = jwtService.generateAccessToken(
                analystUser.getId(),
                analystUser.getEmail(),
                List.copyOf(analystUser.getRoles())
        );

        mockMvc.perform(get("/api/v1/dashboard/summary")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + token))
                .andExpect(status().isForbidden());
    }

    @Test
    void correlationIdIsReturnedInHeader() throws Exception {
        String token = jwtService.generateAccessToken(
                adminUser.getId(),
                adminUser.getEmail(),
                List.copyOf(adminUser.getRoles())
        );

        var result = mockMvc.perform(get("/api/v1/systems")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + token))
                .andExpect(status().isOk())
                .andReturn();

        String correlationId = result.getResponse().getHeader("X-Correlation-Id");
        assertThat(correlationId).isNotBlank();
    }
}
