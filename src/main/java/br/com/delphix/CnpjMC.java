package br.com.delphix;
import com.delphix.masking.api.plugin.MaskingAlgorithm;
import com.delphix.masking.api.plugin.MaskingComponent;
import com.delphix.masking.api.plugin.exception.MaskingException;
import com.delphix.masking.api.plugin.exception.NonConformantDataException;
import com.delphix.masking.api.plugin.referenceType.AlgorithmInstanceReference;
import com.delphix.masking.api.provider.ComponentService;
import com.delphix.masking.api.plugin.utils.GenericData;
import com.delphix.masking.api.plugin.utils.GenericDataRow;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyDescription;

import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class CnpjMC implements MaskingAlgorithm<GenericDataRow> {

    private String name = "CnpjMC";
    /**
     * Get the recommended name of this Algorithm.
     * @return The name of this algorithm
     */
    @Override
    public String getName() {
        return name;
    }

    public String getDescription() {
        return "Algoritmo para mascaramento CM:Numeric de CNPJ armazenados em 3 colunas:raiz, matriz/filial, dígitos verificadores";
    }

    @JsonProperty("chain_algo_ref")
    @JsonPropertyDescription("Reference to AlgorithmInstanceReference of type string > SL ")
    public AlgorithmInstanceReference chain_algo_ref;
    private MaskingAlgorithm<String> chained_algo;

    @JsonProperty("mask_invalid")
    @JsonPropertyDescription("Flag to mask or not invalid numbers. Default False")
    public boolean mask_invalid;

    @Override
    public boolean getAllowFurtherInstances() {
        return true;
    }

    @Override
    public Collection<MaskingComponent> getDefaultInstances() {
        CnpjMC instanceX = new CnpjMC();
        instanceX.name = name;
        instanceX.chain_algo_ref = new AlgorithmInstanceReference("dlpx-core:CM Numeric");
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
    }

    private static final Logger LOGGER = Logger.getLogger("InfoLogging");
    private static final int[] pesoCNPJ = {6, 5, 4, 3, 2, 9, 8, 7, 6, 5, 4, 3, 2};
    public static final String[] BLOCKLIST_CNPJ = new String[] {"00000000000000","11111111111111","22222222222222","33333333333333","44444444444444","55555555555555","66666666666666","77777777777777","88888888888888","99999999999999"};
    public static final String DOCTO_REGEX = "(?<!\\d|-|/|\\.)(\\d{1,2}\\.?\\d{1,3}\\.?\\d{1,3}/?\\d{4}-?\\d{2})(?!\\d|-|/|\\.)";

    public String replaceByMaskedNumbers(String originalValue, String maskedValue){
        int i=0;
        int m=0;
        StringBuilder result = new StringBuilder();

        while (m<maskedValue.length()){
            if (Character.isDigit(originalValue.charAt(i))){
                result.append(maskedValue.charAt(m));
                m++;
            } else {
                result.append(originalValue.charAt(i));
            }
            i++;

        }
        if (i<originalValue.length() && (! Character.isDigit(originalValue.charAt(i))) ){
            result.append(originalValue.charAt(i));
        }


        return result.toString();
    }

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

    public static boolean isCNPJ(String cnpj) {

        if (Arrays.asList(BLOCKLIST_CNPJ).contains(cnpj))
            return(false);
        int digito1 = calcularDigito(cnpj.substring(0,12), pesoCNPJ);
        int digito2 = calcularDigito(cnpj.substring(0,12) + digito1, pesoCNPJ);
        return cnpj.equals(cnpj.substring(0,12) + digito1 + digito2);
    }



    private static boolean empty(final String s) {
        // check for null or empty string
        return s == null || s.trim().isEmpty();
    }

    /**
     * Masks String object
     * @param genericDataRow The String object to be masked. This method should handle null inputs.
     * @return Returns the masked value.
     */
    @Override
    public GenericDataRow mask(@Nullable GenericDataRow genericDataRow) throws MaskingException {

        // read the column values from GenericDataRow
        GenericData rootDataRow = genericDataRow.get("inscricao");
        GenericData branchDataRow = genericDataRow.get("matrizFilial");
        GenericData vdDataRow = genericDataRow.get("digitos");

        String root = rootDataRow.getStringValue();
        String branch = branchDataRow.getStringValue();
        String vd = vdDataRow.getStringValue();


        if (root == null || root.trim().isEmpty() || branch == null || branch.trim().isEmpty() ||  vd == null || vd.trim().isEmpty() ){
            throw new NonConformantDataException("WAR: CNPJ root, branch or verifying digits were null");
        }

        String numbers = padLeftZeros(root, 0)+padLeftZeros(branch,4)+padLeftZeros(vd, 2);
        numbers = numbers.replaceAll("[^\\d]", "");

        if (!numbers.matches(DOCTO_REGEX)||!isCNPJ(padLeftZeros(numbers,14))) {
            throw new NonConformantDataException("WAR: Input is not a valid CNPJ document");
        }

        String maskedRoot ;
        try {
            maskedRoot = chained_algo.mask(root.replaceAll("[^\\d]", ""));
        } catch (MaskingException e){
            throw new NonConformantDataException("WAR: failed to mask CNPJ root");
        }


        StringBuilder maskedNumbers = new StringBuilder();
        maskedNumbers.append(maskedRoot);
        maskedNumbers.append(padLeftZeros(branch, 4));
        Integer digito1 = calcularDigito(padLeftZeros( maskedNumbers.toString(), 12), pesoCNPJ);
        Integer digito2 = calcularDigito(padLeftZeros( maskedNumbers.toString(), 12) + digito1, pesoCNPJ);
        StringBuilder newVd = new StringBuilder();

        StringBuilder result = new StringBuilder();
        result.append(replaceByMaskedNumbers(root, maskedRoot));
        newVd.append(digito1);
        newVd.append(digito2);

        rootDataRow.setValue( result.toString() );
        vdDataRow.setValue( newVd.toString());
        branchDataRow.setValue(branch);
        return genericDataRow;
    }


    @Override
    public Map<String, MaskingType> listMaskedFields() {
        /*
         *  Here we define the column names to be used in the algorithm. These names are only used to reference the
         *  columns within the algorithm and do not need to correspond to the names of the columns on the data source.
         *  For example, our data source may call these 2 fields "dateOfBirth" and "dateOfDeath", however within the
         *  algorithm implementation they will be referenced as "startDate" and "endDate" (see mask method to see how
         *  this is used).
         */
        Map<String, MaskingType> maskedFields = new HashMap<String, MaskingType>();
        maskedFields.put("inscricao", MaskingType.STRING);
        maskedFields.put("matrizFilial", MaskingType.STRING);
        maskedFields.put("digitos", MaskingType.STRING);
        return maskedFields;
    }
}
