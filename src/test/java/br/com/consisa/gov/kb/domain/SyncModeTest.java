package br.com.consisa.gov.kb.domain;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class SyncModeTest {

    private final ObjectMapper mapper = new ObjectMapper();

    @Test
    void serializesDeltaAsDelta() throws Exception {
        assertThat(mapper.writeValueAsString(SyncMode.DELTA)).isEqualTo("\"DELTA\"");
    }

    @Test
    void deserializesDeltaWindowAsDelta() throws Exception {
        SyncMode mode = mapper.readValue("\"DELTA_WINDOW\"", SyncMode.class);
        assertThat(mode).isEqualTo(SyncMode.DELTA);
    }
}
