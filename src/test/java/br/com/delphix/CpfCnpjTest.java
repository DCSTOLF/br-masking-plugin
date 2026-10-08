package br.com.delphix;

import com.delphix.masking.api.plugin.MaskingAlgorithm;
import com.delphix.masking.api.plugin.MaskingComponent;
import com.delphix.masking.api.plugin.exception.MaskingException;
import com.delphix.masking.api.plugin.referenceType.AlgorithmInstanceReference;
import org.junit.Test;

import java.lang.reflect.Field;
import java.lang.reflect.Proxy;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

public class CpfCnpjTest {

    @Test
    public void validatesAlphanumericCnpjCheckDigits() {
        assertTrue(CpfCnpj.isAlphanumericCNPJ("12ABC34501DE35"));
        assertTrue(CpfCnpj.isAlphanumericCNPJ("AB123456789082"));
        assertFalse(CpfCnpj.isAlphanumericCNPJ("12ABC34501DE36"));
        assertFalse(CpfCnpj.isAlphanumericCNPJ("12abc34501DE35"));
    }

    @Test
    public void keepsNumericCnpjValidationUnchanged() {
        assertTrue(CpfCnpj.isAlphanumericCNPJ("11222333000181"));
        assertFalse(CpfCnpj.isAlphanumericCNPJ("00000000000000"));
    }

    @Test
    public void validAlphanumericCnpjUsesBrazilCnpjAlgorithmAndPreservesFormatting() throws Exception {
        AtomicReference<String> valueSentToMasker = new AtomicReference<>();
        CpfCnpj algorithm = new CpfCnpj();
        setField(algorithm, "brazilCnpjAlgo",
                maskingAlgorithm(valueSentToMasker, "AB123456789082"));

        assertEquals("AB.123.456/7890-82", algorithm.mask("12.ABC.345/01DE-35"));
        assertEquals("12ABC34501DE35", valueSentToMasker.get());
    }

    @Test
    public void invalidAlphanumericCnpjIsRejectedBeforeMasking() throws Exception {
        AtomicReference<String> valueSentToMasker = new AtomicReference<>();
        CpfCnpj algorithm = new CpfCnpj();
        setField(algorithm, "brazilCnpjAlgo",
                maskingAlgorithm(valueSentToMasker, "AB123456789082"));

        try {
            algorithm.mask("12.ABC.345/01DE-36");
            fail("Expected invalid CNPJ to be rejected");
        } catch (MaskingException expected) {
            assertNull(valueSentToMasker.get());
        }
    }

    @Test
    public void validCpfUsesBrazilCpfAlgorithmWithCheckDigits() throws Exception {
        AtomicReference<String> valueSentToMasker = new AtomicReference<>();
        CpfCnpj algorithm = new CpfCnpj();
        setField(algorithm, "brazilCpfAlgo",
                maskingAlgorithm(valueSentToMasker, "11144477735"));

        assertEquals("111.444.777-35", algorithm.mask("529.982.247-25"));
        assertEquals("52998224725", valueSentToMasker.get());
    }

    @Test
    public void invalidCpfIsRejectedBeforeMasking() throws Exception {
        AtomicReference<String> valueSentToMasker = new AtomicReference<>();
        CpfCnpj algorithm = new CpfCnpj();
        setField(algorithm, "brazilCpfAlgo",
                maskingAlgorithm(valueSentToMasker, "11144477735"));

        try {
            algorithm.mask("529.982.247-26");
            fail("Expected invalid CPF to be rejected");
        } catch (MaskingException expected) {
            assertNull(valueSentToMasker.get());
        }
    }

    @Test
    public void validNumericCnpjUsesBrazilCnpjAlgorithmWithAllDigits() throws Exception {
        AtomicReference<String> valueSentToMasker = new AtomicReference<>();
        CpfCnpj algorithm = new CpfCnpj();
        setField(algorithm, "brazilCnpjAlgo",
                maskingAlgorithm(valueSentToMasker, "12345678000195"));

        assertEquals("12.345.678/0001-95", algorithm.mask("11.222.333/0001-81"));
        assertEquals("11222333000181", valueSentToMasker.get());
    }

    @Test
    public void invalidNumericCnpjIsRejectedBeforeMasking() throws Exception {
        AtomicReference<String> valueSentToMasker = new AtomicReference<>();
        CpfCnpj algorithm = new CpfCnpj();
        setField(algorithm, "brazilCnpjAlgo",
                maskingAlgorithm(valueSentToMasker, "12345678000195"));

        try {
            algorithm.mask("11.222.333/0001-82");
            fail("Expected invalid CNPJ to be rejected");
        } catch (MaskingException expected) {
            assertNull(valueSentToMasker.get());
        }
    }

    @Test
    public void defaultInstanceReferencesBrazilAlgorithmsByName() {
        CpfCnpj algorithm = (CpfCnpj) new CpfCnpj().getDefaultInstances().iterator().next();

        assertEquals("dlpx-core:Brazil CPF", algorithm.brazil_cpf_algo_ref.getValue());
        assertEquals("dlpx-core:Brazil CNPJ", algorithm.brazil_cnpj_algo_ref.getValue());
    }

    @SuppressWarnings("unchecked")
    private static MaskingAlgorithm<String> maskingAlgorithm(
            AtomicReference<String> valueSentToMasker, String maskedValue) {
        return (MaskingAlgorithm<String>) Proxy.newProxyInstance(
                MaskingAlgorithm.class.getClassLoader(),
                new Class<?>[] {MaskingAlgorithm.class},
                (proxy, method, args) -> {
                    if ("mask".equals(method.getName())) {
                        valueSentToMasker.set((String) args[0]);
                        return maskedValue;
                    }
                    return null;
                });
    }

    private static void setField(Object instance, String name, Object value) throws Exception {
        Field field = instance.getClass().getDeclaredField(name);
        field.setAccessible(true);
        field.set(instance, value);
    }
}
