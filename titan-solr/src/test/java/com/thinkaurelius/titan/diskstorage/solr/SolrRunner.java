package com.thinkaurelius.titan.diskstorage.solr;

import com.google.common.base.Joiner;
import org.apache.commons.io.FileUtils;
import org.apache.solr.cloud.MiniSolrCloudCluster;
import org.apache.solr.cloud.ZkController;
import org.apache.solr.servlet.SolrDispatchFilter;

import java.io.File;

public class SolrRunner {

    protected static final int NUM_SERVERS = 1;
    protected static final String[] COLLECTIONS = new String[] { "store1", "store2", "vertex", "edge", "namev", "namee",
            "composite", "psearch", "esearch", "vsearch", "mi", "mixed", "index1", "index2", "index3",
            "ecategory", "vcategory", "pcategory", "theIndex", "vertices", "edges" };

    protected static final String[] KEY_FIELDS = new String[0];

    private static final String TMP_DIRECTORY = System.getProperty("java.io.tmpdir");
    private static final String TEMPLATE_DIRECTORY = "core-template";

    private static MiniSolrCloudCluster miniSolrCloudCluster;

    public static void start() throws Exception {
        String userDir = System.getProperty("user.dir");
        String solrHome = userDir.contains("titan-solr")
                ? Joiner.on(File.separator).join(userDir, "target", "test-classes", "solr")
                : Joiner.on(File.separator).join(userDir, "titan-solr", "target", "test-classes", "solr");


        File templateDirectory = new File(solrHome + File.separator + TEMPLATE_DIRECTORY);
        assert templateDirectory.exists();

        File temp = new File(TMP_DIRECTORY + File.separator + "solr-" + System.nanoTime());
        assert !temp.exists();

        temp.mkdirs();
        temp.deleteOnExit();

        File solrXml = new File(solrHome, "solr.xml");
        miniSolrCloudCluster = new MiniSolrCloudCluster(NUM_SERVERS, null, solrXml, null, null);

        for (String core : COLLECTIONS) {
            File coreDirectory = new File(temp.getAbsolutePath() + File.separator + core);
            assert coreDirectory.mkdirs();
            FileUtils.copyDirectory(templateDirectory, coreDirectory);
            uploadConfigDirToZk(core, coreDirectory.getAbsolutePath());
        }
    }

    public static MiniSolrCloudCluster getMiniCluster() {
        return miniSolrCloudCluster;
    }

    public static void stop() throws Exception {
        System.clearProperty("solr.solrxml.location");
        System.clearProperty("zkHost");
        miniSolrCloudCluster.shutdown();
    }

    private static ZkController getZkController() {
        SolrDispatchFilter dispatchFilter =
                (SolrDispatchFilter) miniSolrCloudCluster.getJettySolrRunners().get(0).getDispatchFilter().getFilter();
        return dispatchFilter.getCores().getZkController();
    }

    protected static void uploadConfigDirToZk(String coreName, String collectionConfigDir) throws Exception {
        ZkController zkController = getZkController();
        zkController.uploadConfigDir(new File(collectionConfigDir), coreName);
    }
}
