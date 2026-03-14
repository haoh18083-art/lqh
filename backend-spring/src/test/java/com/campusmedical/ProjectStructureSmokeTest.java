package com.campusmedical;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.assertNotNull;

class ProjectStructureSmokeTest {

    @Test
    void applicationClassShouldExist() {
        assertNotNull(CampusMedicalApplication.class);
    }
}
