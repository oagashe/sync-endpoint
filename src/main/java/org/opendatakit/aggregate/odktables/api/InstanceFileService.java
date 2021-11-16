/*
 * Copyright (C) 2012-2013 University of Washington
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */
package org.opendatakit.aggregate.odktables.api;

import java.io.IOException;
import java.util.List;

import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.PathSegment;
import javax.ws.rs.core.Response;

import org.apache.wink.common.model.multipart.InMultiPart;
import org.opendatakit.aggregate.odktables.exception.ODKTablesException;
import org.opendatakit.aggregate.odktables.exception.PermissionDeniedException;
import org.opendatakit.aggregate.odktables.rest.ApiConstants;
import org.opendatakit.aggregate.odktables.rest.entity.OdkTablesFileManifest;
import org.opendatakit.common.persistence.exception.ODKDatastoreException;
import org.opendatakit.common.persistence.exception.ODKTaskLockException;

/**
 * Servlet for handling the uploading and downloading of instance data files
 * (instance attachments) from the phone.
 * <p>
 * The general idea is that the interaction with the actual files will occur at
 * /odktables/{appId}/tables/{tableId}/attachments/{schemaETag}/{rowId}/subpathToFile.
 * <p>
 * The interface only supports puts gets and directory listings at the rowId
 * level.
 * <p>
 * Files will thus be referred to by their unrooted path relative to the
 * /sdcard/opendatakit/{appId}/ directory on the device.
 * <p>
 * A GET request to that url will download the file. A POST request to that url
 * must contain an entity that is the file, as well as a table id parameter on
 * the POST itself.
 * <p>
 * These urls should be generated by a file manifest servlet on a table id
 * basis.
 *
 * @author sudar.sam@gmail.com
 *
 */
public interface InstanceFileService {

  /**
   * The url of the servlet that for downloading and uploading files. This must
   * be appended to the odk table service.
   */
  public static final String SERVLET_PATH = "files";

  public static final String PARAM_AS_ATTACHMENT = "as_attachment";
  public static final String ERROR_MSG_INVALID_ROW_ID = "Invalid RowId.";
  public static final String ERROR_MSG_MULTIPART_MESSAGE_EXPECTED = "Multipart Form expected.";
  public static final String ERROR_MSG_MULTIPART_FILES_ONLY_EXPECTED = "Multipart Form of only file contents expected.";
  public static final String ERROR_MSG_MULTIPART_CONTENT_FILENAME_EXPECTED = "Multipart Form file content must specify instance-relative filename.";
  public static final String ERROR_MSG_MULTIPART_CONTENT_PARSING_FAILED = "Multipart Form parsing failed.";
  public static final String ERROR_MSG_MANIFEST_IS_EMPTY_OR_MISSING = "Supplied manifest is missing or specifies no files (empty).";
  public static final String ERROR_MSG_INSUFFICIENT_PATH = "Not Enough Path Segments: must be at least 1.";
  public static final String ERROR_MSG_UNRECOGNIZED_APP_ID = "Unrecognized app id: ";
  public static final String ERROR_MSG_PATH_NOT_UNDER_APP_ID = "File path is not under app id: ";
  public static final String MIME_TYPE_IMAGE_JPEG = "image/jpeg";

  @GET
  @Path("manifest")
  @Produces({MediaType.APPLICATION_JSON, ApiConstants.MEDIA_TEXT_XML_UTF8, ApiConstants.MEDIA_APPLICATION_XML_UTF8})
  public Response getManifest(@Context HttpHeaders httpHeaders, @QueryParam(PARAM_AS_ATTACHMENT) String asAttachment) throws IOException, ODKTaskLockException, PermissionDeniedException;

  /**
   * The JSON is a OdkTablesFileManifest containing the list of files to be returned.
   * The files are returned in a multipart form data response.
   * 
   * @param httpHeaders
   * @param manifest
   * @return
   * @throws IOException
   * @throws ODKTaskLockException
   * @throws PermissionDeniedException 
   */
  @POST
  @Path("download")
  @Consumes({MediaType.APPLICATION_JSON, ApiConstants.MEDIA_TEXT_XML_UTF8, ApiConstants.MEDIA_APPLICATION_XML_UTF8})
  @Produces({MediaType.MULTIPART_FORM_DATA})
  public Response getFiles(@Context HttpHeaders httpHeaders, OdkTablesFileManifest manifest) throws IOException, ODKTaskLockException, PermissionDeniedException;

  /**
   * Takes a multipart form containing the files to be uploaded.
   * The Content-Disposition for each file should specify the 
   * instance-relative filepath (using forward slashes).
   * If not specified, an error is reported.
   * 
   * @param req
   * @param inMP
   * @return string describing error on failure, otherwise empty and Status.CREATED.
   * @throws IOException
   * @throws ODKTaskLockException
   * @throws ODKTablesException 
   * @throws ODKDatastoreException 
   */
  @POST
  @Path("upload")
  @Consumes({MediaType.MULTIPART_FORM_DATA})
  @Produces({MediaType.APPLICATION_JSON, ApiConstants.MEDIA_TEXT_XML_UTF8, ApiConstants.MEDIA_APPLICATION_XML_UTF8})
  public Response postFiles(@Context HttpServletRequest req, InMultiPart inMP) throws IOException, ODKTaskLockException, ODKTablesException, ODKDatastoreException;
  
  @GET
  @Path("file/{filePath:.*}")
  public Response getFile(@Context HttpHeaders httpHeaders, @PathParam("filePath") List<PathSegment> segments, @QueryParam(PARAM_AS_ATTACHMENT) String asAttachment, @QueryParam("reduceImageSize") String reduceSize) throws IOException, ODKTaskLockException, PermissionDeniedException;

  @POST
  @Path("file/{filePath:.*}")
  @Consumes({MediaType.MEDIA_TYPE_WILDCARD})
  public Response putFile(@Context HttpServletRequest req, @PathParam("filePath") List<PathSegment> segments, byte[] content) throws IOException, ODKTaskLockException, PermissionDeniedException, ODKDatastoreException;

}
