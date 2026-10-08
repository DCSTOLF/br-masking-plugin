package br.com.delphix;

import com.delphix.masking.api.plugin.MaskingAlgorithm;
import com.delphix.masking.api.plugin.MaskingComponent;
import com.delphix.masking.api.plugin.exception.MaskingException;
import com.delphix.masking.api.plugin.exception.NonConformantDataException;
import com.delphix.masking.api.plugin.referenceType.AlgorithmInstanceReference;
import com.delphix.masking.api.provider.ComponentService;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyDescription;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.Arrays;
import java.util.Collection;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;


public class CpfCnpj implements MaskingAlgorithm<String> {

    private String name = "CpfCnpj";

    /**
     * Get the recommended name of this Algorithm.
     * @return The name of this algorithm
     */
    @Override
    public String getName() {
        return name;
    }

    public String getDescription() {
        return "Algoritmo para mascaramento CM:Numeric de CPF ou CNPJ armazenados em colunas únicas";
    }

    @Override
    public boolean getAllowFurtherInstances() {
        return true;
    }

    @JsonProperty("chain_algo_ref")
    @JsonPropertyDescription("Reference to AlgorithmInstanceReference of type string > SL ")
    public AlgorithmInstanceReference chain_algo_ref;
    @JsonProperty("brazil_cpf_algo_ref")
    @JsonPropertyDescription("Named algorithm instance used to mask valid CPFs")
    public AlgorithmInstanceReference brazil_cpf_algo_ref;
    @JsonProperty("brazil_cnpj_algo_ref")
    @JsonPropertyDescription("Named algorithm instance used to mask valid CNPJs")
    public AlgorithmInstanceReference brazil_cnpj_algo_ref;
    private MaskingAlgorithm<String> chained_algo;
    private MaskingAlgorithm<String> brazilCpfAlgo;
    private MaskingAlgorithm<String> brazilCnpjAlgo;

    @JsonProperty("mask_invalid")
    @JsonPropertyDescription("Flag to mask or not invalid numbers. Default False")
    public boolean mask_invalid;

    @Override
    public Collection<MaskingComponent> getDefaultInstances() {
        CpfCnpj instanceX = new CpfCnpj();
        instanceX.name = name;
        instanceX.chain_algo_ref = new AlgorithmInstanceReference("dlpx-core:CM Numeric");
        instanceX.brazil_cpf_algo_ref = new AlgorithmInstanceReference("dlpx-core:Brazil CPF");
        instanceX.brazil_cnpj_algo_ref = new AlgorithmInstanceReference("dlpx-core:Brazil CNPJ");
        instanceX.mask_invalid = false;
        return Arrays.asList(instanceX);
    }

    @Override
    public void setup(@Nonnull ComponentService serviceProvider) {
        try {
            chained_algo = serviceProvider.getAlgorithmByName(chain_algo_ref, MaskingType.STRING);
        } catch (Exception e){
            e.printStackTrace();
        }
        try {
            brazilCpfAlgo = serviceProvider.getAlgorithmByName(
                    brazil_cpf_algo_ref, MaskingType.STRING);
        } catch (Exception e) {
            LOGGER.log(Level.WARNING, "Unable to load configured Brazil CPF algorithm instance; CPF masking will be unavailable", e);
        }
        try {
            brazilCnpjAlgo = serviceProvider.getAlgorithmByName(
                    brazil_cnpj_algo_ref, MaskingType.STRING);
        } catch (Exception e) {
            LOGGER.log(Level.WARNING, "Unable to load configured Brazil CNPJ algorithm instance; CNPJ masking will be unavailable", e);
        }
    }

    private static final Logger LOGGER = Logger.getLogger("InfoLogging");

    private static final int[] pesoCPF = {11, 10, 9, 8, 7, 6, 5, 4, 3, 2};

    private static final int[] pesoCNPJ = {6, 5, 4, 3, 2, 9, 8, 7, 6, 5, 4, 3, 2};

    public static final String[] BLOCKLIST_CPF = new String[] {"00000000000","11111111111","22222222222","33333333333","44444444444","55555555555","66666666666","77777777777","88888888888","99999999999"};

    public static final String[] BLOCKLIST_CNPJ = new String[] {"00000000000000","11111111111111","22222222222222","33333333333333","44444444444444","55555555555555","66666666666666","77777777777777","88888888888888","99999999999999"};

    public static final String DOCTO_REGEX = "^\\s*(?<!\\d|-|/|\\.)(?:(?:\\d{1,2}\\.?\\d{1,3}\\.?\\d{1,3}/?\\d{4}-?\\d{2})|(?:[A-Z0-9]{2}\\.?[A-Z0-9]{3}\\.?[A-Z0-9]{3}/?[A-Z0-9]{4}-?\\d{2})|(?:\\d{0,3}\\.?\\d{0,3}\\.?\\d{1,3}[-.]?\\d{2}))(?!\\d|-|/|\\.)\\s*$";
    private static final String ALPHANUMERIC_CNPJ_REGEX = "^[A-Z0-9]{12}\\d{2}$";




    public static String padLeftZeros(String inputString, int length) {
        if (inputString.length() >= length) {
            return inputString;
        }
        StringBuilder sb = new StringBuilder();
        while (sb.length() < length - inputString.length()) {
            sb.append('0');
        }
        sb.append(inputString);

        return sb.toString();
    }

    private static String padLeft(String text, char character) {
        return String.format("%11s", text).replace(' ', character);
    }

    private static int calcularDigito(String str, int[] peso) {
        int soma = 0;
        for (int indice=str.length()-1, digito; indice >= 0; indice-- ) {
            digito = Integer.parseInt(str.substring(indice,indice+1));
            soma += digito*peso[peso.length-str.length()+indice];
        }
        soma = 11 - soma % 11;
        return soma > 9 ? 0 : soma;
    }



    public static boolean isCPF(String cpf) {

        if (Arrays.asList(BLOCKLIST_CPF).contains(cpf))
            return(false);

        for (int j = 0; j < 10; j++)
            if (padLeft(Integer.toString(j), Character.forDigit(j, 10)).equals(cpf))
                return false;

        int digito1 = calcularDigito(cpf.substring(0,9), pesoCPF);
        int digito2 = calcularDigito(cpf.substring(0,9) + digito1, pesoCPF);
        return cpf.equals(cpf.substring(0,9) + digito1 + digito2);
    }

    public static boolean isCNPJ(String cnpj) {

        if (Arrays.asList(BLOCKLIST_CNPJ).contains(cnpj))
            return(false);
        int digito1 = calcularDigito(cnpj.substring(0,12), pesoCNPJ);
        int digito2 = calcularDigito(cnpj.substring(0,12) + digito1, pesoCNPJ);
        return cnpj.equals(cnpj.substring(0,12) + digito1 + digito2);
    }

    private static int calcularDigitoCnpjAlfanumerico(String prefix) {
        int soma = 0;
        for (int indice = prefix.length() - 1; indice >= 0; indice--) {
            int value = prefix.charAt(indice) - '0';
            soma += value * pesoCNPJ[pesoCNPJ.length - prefix.length() + indice];
        }
        soma = 11 - soma % 11;
        return soma > 9 ? 0 : soma;
    }

    public static boolean isAlphanumericCNPJ(String cnpj) {
        if (cnpj == null || !cnpj.matches(ALPHANUMERIC_CNPJ_REGEX)) {
            return false;
        }
        if (cnpj.matches("\\d{14}")) {
            return isCNPJ(cnpj);
        }

        int digito1 = calcularDigitoCnpjAlfanumerico(cnpj.substring(0, 12));
        int digito2 = calcularDigitoCnpjAlfanumerico(cnpj.substring(0, 12) + digito1);
        return cnpj.equals(cnpj.substring(0, 12) + digito1 + digito2);
    }

    private static String replaceByMaskedCharacters(String original, String masked) throws NonConformantDataException {
        StringBuilder result = new StringBuilder(original.length());
        int maskedIndex = 0;
        for (int i = 0; i < original.length(); i++) {
            char current = original.charAt(i);
            if (Character.isLetterOrDigit(current)) {
                if (maskedIndex >= masked.length()) {
                    throw new NonConformantDataException("WAR: Brazil masking algorithm returned an invalid value");
                }
                result.append(masked.charAt(maskedIndex++));
            } else {
                result.append(current);
            }
        }
        if (maskedIndex != masked.length()) {
            throw new NonConformantDataException("WAR: Brazil masking algorithm returned an invalid value");
        }
        return result.toString();
    }



    private static boolean empty(final String s) {
        // check for null or empty string
        return s == null || s.trim().isEmpty();
    }

    /**
     * Masks String object
     * @param input The String object to be masked. This method should handle null inputs.
     * @return Returns the masked value.
     */
    @Override
    public String mask(@Nullable String input) throws MaskingException{

        if (empty(input)) {
            return input;
        }

        if (!(input.matches(DOCTO_REGEX))) {
            throw new NonConformantDataException("WAR: Input doesn't match a document regex");
        }
        String alphanumericCnpj = input.replaceAll("[^A-Z0-9]", "");
        if (alphanumericCnpj.matches(".*[A-Z].*")) {
            if (!isAlphanumericCNPJ(alphanumericCnpj)) {
                throw new NonConformantDataException("WAR: Input is not a valid CNPJ document");
            }
            if (brazilCnpjAlgo == null) {
                throw new NonConformantDataException("WAR: dlpx-core:Brazil CNPJ algorithm is unavailable");
            }
            String maskedCnpj = brazilCnpjAlgo.mask(alphanumericCnpj);
            if (!isAlphanumericCNPJ(maskedCnpj)) {
                throw new NonConformantDataException("WAR: Brazil CNPJ algorithm returned an invalid CNPJ");
            }
            return replaceByMaskedCharacters(input, maskedCnpj);
        }

        String numbers = input.replaceAll("[^\\d]", "");
        if (isCPF(padLeftZeros(numbers, 11))) {
            if (brazilCpfAlgo == null) {
                throw new NonConformantDataException("WAR: dlpx-core:Brazil CPF algorithm is unavailable");
            }
            String cpf = padLeftZeros(numbers, 11);
            String maskedCpf = brazilCpfAlgo.mask(cpf);
            if (!isCPF(maskedCpf)) {
                throw new NonConformantDataException("WAR: Brazil CPF algorithm returned an invalid CPF");
            }
            String visibleMaskedCpf = maskedCpf.substring(maskedCpf.length() - numbers.length());
            return replaceByMaskedCharacters(input, visibleMaskedCpf);
        }

        if (isCNPJ(padLeftZeros(numbers,14))){
            if (brazilCnpjAlgo == null) {
                throw new NonConformantDataException("WAR: dlpx-core:Brazil CNPJ algorithm is unavailable");
            }
            String cnpj = padLeftZeros(numbers, 14);
            String maskedCnpj = brazilCnpjAlgo.mask(cnpj);
            if (!maskedCnpj.matches("\\d{14}") || !isCNPJ(maskedCnpj)) {
                throw new NonConformantDataException("WAR: Brazil CNPJ algorithm returned an invalid CNPJ");
            }
            String visibleMaskedCnpj = maskedCnpj.substring(maskedCnpj.length() - numbers.length());
            return replaceByMaskedCharacters(input, visibleMaskedCnpj);
        }
        else
            throw new NonConformantDataException("WAR: Input is not a valid document");
    }
}
