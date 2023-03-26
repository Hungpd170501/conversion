package com.se1605.service;

import com.se1605.config.ConversionConfiguration;
import com.se1605.model.request.ConversionPostedData;
import com.se1605.model.request.FileTreeRequest;
import com.se1605.model.response.ConversionTypesEntity;
import org.springframework.http.ResponseEntity;

import java.io.IOException;
import java.util.List;

public interface ConversionService {

    /**
     * Load list of elements from directory
     *
     * @param fileTreeRequest request with path to directory
     * @return list of files and folders
     */
    List<ConversionTypesEntity> loadFiles(FileTreeRequest fileTreeRequest);

    /**
     * Get configuration
     *
     * @return configuration
     */
    ConversionConfiguration getConversionConfiguration();

    /**
     * Convert file to specified format
     *
     * @param postedData request with that specifies format and file to be converted
     */
    void convert(ConversionPostedData postedData, String key);


    /**
     * Creates response for downloading converted file
     *
     * @param path coverted file name
     * @return ResponseEntity
     * @throws IOException
     */
    ResponseEntity download(String path) throws IOException;
}
