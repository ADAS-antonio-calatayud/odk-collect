package org.odk.collect.android.formmanagement.download

import org.apache.commons.io.FileUtils.writeByteArrayToFile
import org.hamcrest.CoreMatchers.equalTo
import org.hamcrest.MatcherAssert.assertThat
import org.junit.Test
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.mock
import org.odk.collect.android.formmanagement.ServerFormDetails
import org.odk.collect.android.utilities.FileUtils
import org.odk.collect.entities.InMemEntitiesRepository
import org.odk.collect.forms.Form
import org.odk.collect.forms.FormSource
import org.odk.collect.forms.ManifestFile
import org.odk.collect.forms.MediaFile
import org.odk.collect.formstest.FormFixtures
import org.odk.collect.formstest.FormUtils.createXFormFile
import org.odk.collect.formstest.InMemFormsRepository
import org.odk.collect.shared.TempFiles.createTempDir
import org.odk.collect.shared.TempFiles.createTempFile
import org.odk.collect.shared.strings.Md5
import java.io.File

class FormDownloadUseCasesTest {
    @Test
    fun `copySavedFileFromPreviousFormVersionIfExists does not copy any file if there is no matching last-saved file`() {
        val destinationMediaDirPath = createTempDir().absolutePath
        FormDownloadUseCases.copySavedFileFromPreviousFormVersionIfExists(InMemFormsRepository(), "1", destinationMediaDirPath)

        val resultFile = File(destinationMediaDirPath, FileUtils.LAST_SAVED_FILENAME)
        assertThat(resultFile.exists(), equalTo(false))
    }

    @Test
    fun `copySavedFileFromPreviousFormVersionIfExists copies the newest matching last-saved file for given formId`() {
        val tempDir1 = createTempDir()
        val file1 = createTempFile(tempDir1, "last-saved", ".xml")
        writeByteArrayToFile(file1, "file1".toByteArray())

        val tempDir2 = createTempDir()
        val file2 = createTempFile(tempDir2, "last-saved", ".xml")
        writeByteArrayToFile(file2, "file2".toByteArray())

        val tempDir3 = createTempDir()
        val file3 = createTempFile(tempDir3, "last-saved", ".xml")
        writeByteArrayToFile(file3, "file3".toByteArray())

        val tempDir4 = createTempDir()
        val file4 = createTempFile(tempDir4, "last-saved", ".xml")
        writeByteArrayToFile(file4, "file4".toByteArray())

        val formsRepository = InMemFormsRepository().also {
            it.save(
                Form.Builder()
                    .dbId(1)
                    .formId("1")
                    .version("1")
                    .date(0)
                    .formFilePath(createXFormFile("1", "1").absolutePath)
                    .formMediaPath(file1.parent)
                    .build()
            )

            it.save(
                Form.Builder()
                    .dbId(2)
                    .formId("1")
                    .version("2")
                    .date(2)
                    .formFilePath(createXFormFile("1", "2").absolutePath)
                    .formMediaPath(file2.parent)
                    .build()
            )

            it.save(
                Form.Builder()
                    .dbId(3)
                    .formId("1")
                    .version("3")
                    .date(1)
                    .formFilePath(createXFormFile("1", "3").absolutePath)
                    .formMediaPath(file3.parent)
                    .build()
            )

            it.save(
                Form.Builder()
                    .dbId(4)
                    .formId("2")
                    .version("1")
                    .date(3)
                    .formFilePath(createXFormFile("2", "1").absolutePath)
                    .formMediaPath(file4.parent)
                    .build()
            )
        }

        val destinationMediaDirPath = createTempDir().absolutePath
        FormDownloadUseCases.copySavedFileFromPreviousFormVersionIfExists(formsRepository, "1", destinationMediaDirPath)

        val resultFile = File(destinationMediaDirPath, FileUtils.LAST_SAVED_FILENAME)
        assertThat(resultFile.readText(), equalTo("file2"))
    }

    @Test
    fun `downloadMediaFiles returns false when there is an existing copy of a media file and an older one`() {
        var date: Long = 0
        // Save forms
        val formsRepository = InMemFormsRepository {
            date += 1
            date
        }
        val form1 = FormFixtures.form(
            version = "1",
            mediaFiles = listOf(Pair("file", "old"))
        )
        formsRepository.save(form1)

        val form2 = FormFixtures.form(
            version = "2",
            mediaFiles = listOf(Pair("file", "existing"))
        )
        formsRepository.save(form2)

        // Set up same media file on server
        val existingMediaFileHash = Md5.getMd5Hash(File(form2.formMediaPath, "file"))!!
        val mediaFile = MediaFile("file", existingMediaFileHash, "downloadUrl")
        val manifestFile = ManifestFile(null, listOf(mediaFile))
        val serverFormDetails =
            ServerFormDetails(null, null, "formId", "3", null, false, true, manifestFile)
        val formSource = mock<FormSource> {
            on { fetchMediaFile(mediaFile.downloadUrl) } doReturn "existing".toByteArray()
                .inputStream()
        }

        val result = FormDownloadUseCases.downloadMediaFiles(
            serverFormDetails,
            formSource,
            formsRepository,
            File(createTempDir(), "temp").absolutePath,
            createTempDir(),
            InMemEntitiesRepository(),
            mock()
        )

        assertThat(result, equalTo(false))
    }

    @Test
    fun `downloadMediaFiles returns false when there is an existing copy of a media file and an older one and media file list hash doesn't match existing copy`() {
        // Save forms
        var date: Long = 0
        val formsRepository = InMemFormsRepository {
            date += 1
            date
        }
        val form1 = FormFixtures.form(
            version = "1",
            mediaFiles = listOf(Pair("file", "old"))
        )
        formsRepository.save(form1)

        val form2 = FormFixtures.form(
            version = "2",
            mediaFiles = listOf(Pair("file", "existing"))
        )
        formsRepository.save(form2)

        // Set up same media file on server
        val mediaFile = MediaFile("file", "somethingElse", "downloadUrl")
        val manifestFile = ManifestFile(null, listOf(mediaFile))
        val serverFormDetails =
            ServerFormDetails(null, null, "formId", "3", null, false, true, manifestFile)
        val formSource = mock<FormSource> {
            on { fetchMediaFile(mediaFile.downloadUrl) } doReturn "existing".toByteArray()
                .inputStream()
        }

        val result = FormDownloadUseCases.downloadMediaFiles(
            serverFormDetails,
            formSource,
            formsRepository,
            File(createTempDir(), "temp").absolutePath,
            createTempDir(),
            InMemEntitiesRepository(),
            mock()
        )

        assertThat(result, equalTo(false))
    }
}
