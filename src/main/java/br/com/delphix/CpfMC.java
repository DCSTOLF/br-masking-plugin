package br.com.delphix;

import com.delphix.masking.api.plugin.MaskingAlgorithm;
import com.delphix.masking.api.plugin.MaskingComponent;
import com.delphix.masking.api.plugin.exception.MaskingException;
import com.delphix.masking.api.plugin.exception.NonConformantDataException;
import com.delphix.masking.api.plugin.referenceType.AlgorithmInstanceReference;
import com.delphix.masking.api.plugin.utils.GenericData;
import com.delphix.masking.api.plugin.utils.GenericDataRow;
import com.delphix.masking.api.provider.ComponentService;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyDescription;

import java.lang.String;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Logger;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;

public class CpfMC implements MaskingAlgorithm<GenericDataRow> {

    private String name = "CpfMC";

    /**
     * Get the recommended name of this Algorithm.
     * @return The name of this algorithm
     */
    @Override
    public String getName() {
        // TODO: Change this if you'd like to name your algorithm differently from the Java class.
        return name;
    }

    public String getDescription() {
        return "Algoritmo para mascaramento CM:Numeric de CPF armazenados em 2 colunas:raiz, dígitos verificadores";
    }

    @Override
    public boolean getAllowFurtherInstances() {
        return true;
    }

    @JsonProperty("chain_algo_ref")
    @JsonPropertyDescription("Reference to AlgorithmInstanceReference of type string > SL ")
    public AlgorithmInstanceReference chain_algo_ref;
    private MaskingAlgorithm<String> chained_algo;

    @JsonProperty("mask_invalid")
    @JsonPropertyDescription("Flag to mask or not invalid numbers. Default False")
    public boolean mask_invalid;

    @Override
    public Collection<MaskingComponent> getDefaultInstances() {
        CpfMC instanceX = new CpfMC();
        instanceX.name = name;
        instanceX.mask_invalid = false;
        instanceX.chain_algo_ref = new AlgorithmInstanceReference("dlpx-core:CM Numeric");
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
    public static final String[] BLOCKLIST_CPF = new String[] {"00000000000","11111111111","22222222222","33333333333","44444444444","55555555555","66666666666","77777777777","88888888888","99999999999"};
    private static final int[] pesoCPF = {11, 10, 9, 8, 7, 6, 5, 4, 3, 2};


    private static String padLeft(String text, char character) {
        return String.format("%11s", text).replace(' ', character);
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

    private static int calcularDigito(String str, int[] peso) {
        int soma = 0;
        for (int indice=str.length()-1, digito; indice >= 0; indice-- ) {
            digito = Integer.parseInt(str.substring(indice,indice+1));
            soma += digito*peso[peso.length-str.length()+indice];
        }
        soma = 11 - soma % 11;
        return soma > 9 ? 0 : soma;
    }

    public static final String DOCTO_REGEX = "(?<!\\d|-|/|\\.)(\\d{0,3}\\.?\\d{0,3}\\.?\\d{1,3}[-.]?\\d{2})(?!\\d|-|/|\\.)";
    /**
     * Masks String object
     * @param genericDataRow The String object to be masked. This method should handle null inputs.
     * @return Returns the masked value.
     */
    @Override
    public GenericDataRow mask(@Nullable GenericDataRow genericDataRow) throws MaskingException {
        // read the column values from GenericDataRow
        GenericData rootDataRow = genericDataRow.get("inscricao");
        GenericData vdDataRow = genericDataRow.get("digitos");

        String root = rootDataRow.getStringValue();
        String vd = vdDataRow.getStringValue();


        if (root == null || root.trim().isEmpty() ||  vd == null || vd.trim().isEmpty() ){
            throw new NonConformantDataException("WAR: CPF root or verifying digits were null");
        }

        String numbers = padLeftZeros(root,9)+padLeftZeros(vd, 2);
//        LOGGER.info(numbers);
        if (!numbers.matches(DOCTO_REGEX)||!isCPF(padLeftZeros(numbers,11))) {
            throw new NonConformantDataException("WAR: Input is not a valid CPF document");
        }

        String maskedRoot ;
        try {
            maskedRoot = chained_algo.mask(root.replaceAll("[^\\d]", ""));
        } catch (MaskingException e){
            throw new NonConformantDataException("WAR: failed to mask CPF root");
        }
        StringBuilder maskedNumbers = new StringBuilder();
        maskedNumbers.append(maskedRoot);
        LOGGER.info(maskedRoot.toString());
        Integer digito1 = calcularDigito(padLeftZeros( maskedNumbers.toString(), 9), pesoCPF);
        Integer digito2 = calcularDigito(padLeftZeros( maskedNumbers.toString(), 9) + digito1, pesoCPF);
        StringBuilder newVd = new StringBuilder();

        int i =0;
        int m = 0;
        StringBuilder result = new StringBuilder();
        while (m<maskedRoot.length()){
            if (Character.isDigit(root.toString().charAt(i))){
                result.append(maskedRoot.charAt(m));
                m++;
            } else {
                result.append(root.toString().charAt(i));
            }
            i++;

        }
        if (i<root.toString().length() && (! Character.isDigit(root.toString().charAt(i))) ){
            result.append(root.toString().charAt(i));
        }
        newVd.append(digito1);
        newVd.append(digito2);

        rootDataRow.setValue( maskedRoot );
        vdDataRow.setValue( newVd.toString());
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
        maskedFields.put("digitos", MaskingType.STRING);
        return maskedFields;
    }
}
