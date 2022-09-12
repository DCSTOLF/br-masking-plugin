package br.com.delphix;

import com.delphix.masking.api.plugin.MaskingAlgorithm;
import com.delphix.masking.api.plugin.MaskingComponent;
import com.delphix.masking.api.plugin.exception.MaskingException;
import com.delphix.masking.api.plugin.exception.NonConformantDataException;
import com.delphix.masking.api.plugin.referenceType.AlgorithmInstanceReference;
import com.delphix.masking.api.provider.ComponentService;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyDescription;

import com.google.common.base.Strings;
import java.lang.String;
import java.lang.reflect.Array;
import java.util.Arrays;
import java.util.Collection;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.logging.Logger;

public class DDD implements MaskingAlgorithm<String> {


    private String name = "DDD";

    /**
     * Get the recommended name of this Algorithm.
     * @return The name of this algorithm
     */
    @Override
    public String getName() {
        return name;
    }

    public String getDescription() {
        return "Algoritmo para mascaramento CM:Numeric para DDDs armazenados em colunas únicas";
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
        DDD instanceX = new DDD();
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

    private static boolean empty(final String s) {
        // check for null or empty string
        return s == null || s.trim().isEmpty();
    }


    private static final Logger LOGGER = Logger.getLogger("InfoLogging");

    private static final int[][] VALID_LOCAL_CODES = {
            {},
            {1,2,3,4,5,6,7,8,9},
            {1,2,4,7,8},
            {1,2,3,4,5,7,8},
            {1,2,3,4,5,6,7,8,9},
            {1,3,4,5},
            {1,2,3,4,5,6,7,8,9},
            {1,3,4,5,7,9},
            {1,2,3,4,5,6,7,8,9},
            {1,2,3,4,5,6,7,8,9},
            {1,2,3,4,5,6,7,8,9}
    };

    public static final String DDD_REGEX = "^\\s*(?<!\\d|-|\\/|\\.)(?:\\(?)(0?(?:1[1-9]|2[1,2,4,7,8]|3[1-5,7,8]|4[1-9]|5[1,3,4,5]|6[1-9]|7[1,3,4,5,7,9]|8[1-9]|9[1-9]))?(?:\\)?)(?!\\d|-|\\/|\\.)\\s*$";

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

    public String maskAreaCode(@Nullable String input) throws MaskingException {
        if (empty(input)) {
            return input;
        }
        String code = input.replaceAll("[^\\d]", "");

        LOGGER.info("input "+input);
        int n = Integer.parseInt(code);
        LOGGER.info("code "+code);
        String maskedNumbers = chained_algo.mask(Integer.toString(n)) ;

        int firstDigit=Character.getNumericValue(maskedNumbers.charAt(0));
        int[] tmp = VALID_LOCAL_CODES[firstDigit];
        int secondDigit=tmp[(tmp.length-1)%firstDigit];




        StringBuilder maskedCode = new StringBuilder();
        if (code.length()==3 || code.charAt(0)==0){
            maskedCode.append(code.charAt(0));
        }
        maskedCode.append(firstDigit);
        maskedCode.append(secondDigit);

        String result = replaceByMaskedNumbers(input, maskedCode.toString());

        return result;
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

        if (!(input.matches(DDD_REGEX))) {
            throw new NonConformantDataException("WAR: Input is not a valid area code");
        }

        Pattern p = Pattern.compile(DDD_REGEX);
        Matcher m = p.matcher(input);
        StringBuffer result = new StringBuffer();
        while (m.find()) {
            m.appendReplacement(result, maskAreaCode(m.group(0)));
        }
        m.appendTail(result);
        return result.toString();
    }


}
