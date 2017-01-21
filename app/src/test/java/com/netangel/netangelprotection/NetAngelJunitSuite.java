package com.netangel.netangelprotection;

import org.junit.Before;
import org.mockito.MockitoAnnotations;

public abstract class NetAngelJunitSuite {

    @Before
    public final void setup() {
        MockitoAnnotations.initMocks(this);
    }

}
