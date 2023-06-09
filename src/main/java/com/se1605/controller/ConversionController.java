package com.se1605.controller;

import com.se1605.config.ConversionConfiguration;
import com.se1605.config.GlobalConfiguration;
import com.se1605.model.request.ConversionPostedData;
import com.se1605.model.request.FileTreeRequest;
import com.se1605.model.response.ConversionTypesEntity;
import com.se1605.model.response.UploadedDocumentEntity;
import com.se1605.service.ConversionService;
import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import javax.annotation.Nullable;
import java.util.List;
import java.util.Map;

import static com.se1605.util.Utils.setLocalPort;
import static com.se1605.util.Utils.uploadFile;
import static org.springframework.http.MediaType.APPLICATION_JSON_VALUE;
import static org.springframework.http.MediaType.MULTIPART_FORM_DATA_VALUE;

@Controller
@RequestMapping("/conversion")
public class ConversionController {
    private static final Logger logger = LoggerFactory.getLogger(ConversionController.class);

    @Autowired
    public GlobalConfiguration globalConfiguration;


    @Autowired
    public ConversionService conversionService;

    @RequestMapping(method = RequestMethod.GET, value = "/loadConfig", produces = APPLICATION_JSON_VALUE)
    @ResponseBody
    public ConversionConfiguration loadConfig() {
        return conversionService.getConversionConfiguration();
    }


    /**
     * Get conversion page
     *
     * @param model model data for template
     * @return template name
     */
    @RequestMapping(method = RequestMethod.GET)
    public String getView(HttpServletRequest request, Map<String, Object> model) {
        setLocalPort(request, globalConfiguration.getServer());

        model.put("globalConfiguration", globalConfiguration);
        logger.debug("conversion config: {}", conversionService.getConversionConfiguration());
        model.put("conversionConfiguration", conversionService.getConversionConfiguration());
        return "conversion";
    }

    /**
     * Get files and directories
     *
     * @return files and directories list
     */
    @RequestMapping(method = RequestMethod.POST, value = "/loadFileTree", consumes = APPLICATION_JSON_VALUE, produces = APPLICATION_JSON_VALUE)
    @ResponseBody
    public List<ConversionTypesEntity> loadFileTree(@RequestBody FileTreeRequest fileTreeRequest) {
        return conversionService.loadFiles(fileTreeRequest);
    }


    /**
     * Upload document
     *
     * @return uploaded document object (the object contains uploaded document guid)
     */
    @RequestMapping(method = RequestMethod.POST, value = "/uploadDocument",
            consumes = MULTIPART_FORM_DATA_VALUE, produces = APPLICATION_JSON_VALUE)
    @ResponseBody
    public UploadedDocumentEntity uploadDocument(@Nullable @RequestParam("file") MultipartFile content,
                                                 @RequestParam(value = "url", required = false) String url,
                                                 @RequestParam("rewrite") Boolean rewrite) {
        // get documents storage path
        String documentStoragePath = conversionService.getConversionConfiguration().getFilesDirectory();
        // save the file
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        String pathname = uploadFile(documentStoragePath, content, url, rewrite, authentication.getName());
        // create response data
        UploadedDocumentEntity uploadedDocument = new UploadedDocumentEntity();
        uploadedDocument.setGuid(pathname);
        return uploadedDocument;
    }

    @RequestMapping(method = RequestMethod.POST, value = "/convert",
            consumes = MediaType.APPLICATION_JSON_VALUE, produces = APPLICATION_JSON_VALUE)
    @ResponseBody
    public ResponseEntity convert(@RequestBody ConversionPostedData postedData) {
        try {
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
            conversionService.convert(postedData, authentication.getName());
            return ResponseEntity.ok().build();
        } catch (Exception e) {
            return ResponseEntity.ok().build();
        }
    }


    @RequestMapping(method = RequestMethod.GET, value = "/downloadDocument/")
    @ResponseBody
    public ResponseEntity downloadDocument(@RequestParam String path) {
        try {
            return conversionService.download(path);
        } catch (Exception e) {
            logger.error(e.getMessage(), e);
            return ResponseEntity.unprocessableEntity().build();
        }
    }
}
