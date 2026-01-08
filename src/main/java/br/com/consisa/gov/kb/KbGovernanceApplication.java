package br.com.consisa.gov.kb;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Classe principal da aplicação KB Governance.
 *
 * Responsabilidade:
 * - Inicializar o contexto Spring Boot
 * - Fazer o scan dos componentes (@Component, @Service, @Repository, @Controller)
 * - Subir a aplicação e expor os endpoints REST
 *
 * Este é o ponto de entrada da aplicação.
 */
@SpringBootApplication
public class KbGovernanceApplication {

    /**
     * Método main responsável por iniciar a aplicação.
     *
     * O Spring Boot:
     * - Cria o ApplicationContext
     * - Configura Beans automaticamente
     * - Inicializa servidores web (ex: Tomcat embutido)
     */
    public static void main(String[] args) {
        SpringApplication.run(KbGovernanceApplication.class, args);
    }
}
