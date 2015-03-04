package com.app3.jjg

import org.jooq.util.GeneratorStrategy
import org.jooq.util.JavaGenerator
import org.jooq.util.SchemaDefinition

/**
 * Extends the built-in JOOQ generator in order to generate a couple extra beans
 * that are handy for people working with JHipster (Spring Boot).
 */
class JooqJhipsterGenerator extends JavaGenerator {

    class Bucket {
        String fullRecordName
        String daoName
        String fullDaoFileName
        String recordName
        String fullDaoName

        @Override
        public String toString() {
            return "Bucket{" +
                    "fullRecordName='" + fullRecordName + '\'' +
                    ", daoName='" + daoName + '\'' +
                    ", fullDaoFileName='" + fullDaoFileName + '\'' +
                    ", recordName='" + recordName + '\'' +
                    ", fullDaoName='" + fullDaoName + '\'' +
                    '}';
        }
    }

    List<Bucket> buckets = []
    String outdir
    def header = """
/**
 * Generated by JooqJhipsterGenerator.  DO NOT EDIT.
 * Generated at ${new Date().toTimestamp().toString()}
 *
 * Generator written by will.mitchell@gmail.com
 * License: http://www.apache.org/licenses/LICENSE-2.0.txt
 * Use at your own risk.
 */
"""
    def parentPackage
    def parentDir

    @Override
    protected void generateSchema(SchemaDefinition schema) {
        println "In generate schema, name: ${schema.outputName}"

        buckets = schema.tables
                .findAll {
            def name = getStrategy().getJavaClassName(it, GeneratorStrategy.Mode.DAO)
            !(name.toLowerCase().contains("databasechange"))
        }
        .collect { table ->
            new Bucket(
                    fullRecordName: getStrategy().getFullJavaClassName(table, GeneratorStrategy.Mode.RECORD),
                    recordName: getStrategy().getJavaClassName(table, GeneratorStrategy.Mode.RECORD),
                    daoName: getStrategy().getJavaClassName(table, GeneratorStrategy.Mode.DAO),
                    fullDaoName: getStrategy().getFullJavaClassName(table, GeneratorStrategy.Mode.DAO),
                    fullDaoFileName: getStrategy().getFileName(table, GeneratorStrategy.Mode.DAO)
            )
        }
        // Note that the JOOQ generator will erase whatever we write, so we have to put these files
        // in the parent directory.

        println "target package: ${strategy.targetPackage}"
        parentPackage = strategy.targetPackage.split(/\./)[0..-2].join('.')
        parentDir = parentPackage.replace('.', '/')
        println "parent dir: ${parentDir}"

        outdir = "src/main/generated/${parentDir}"
        println "outdir: $outdir"
        generateCustom()

        super.generateSchema(schema)
    }

    void generateCustom() {
        println "Time to generate code for: ${buckets}"
        def decls = buckets.collect {

            """
    @Bean
    public ${it.fullDaoName} get${it.daoName}(){
        return new ${it.fullDaoName}(configuration);
    }

"""
        }.join('\n')

        new File(outdir).mkdirs()
        assert new File(outdir).directory

        String fname = "${outdir}/JooqDaoFactory.java"
        println "Generating file: ${fname}"

        def JooqDaoFactory = """package ${parentPackage};

import org.jooq.Configuration;
import org.springframework.context.annotation.Bean;
import javax.inject.Inject;

${header}
@org.springframework.context.annotation.Configuration
public class JooqDaoFactory {

    @Inject
    Configuration configuration;

    ${decls}

}
"""
        try {
            new File(fname).text = JooqDaoFactory
            def file = new File(fname)
            println "Full path: " + file.absolutePath + " size: " + file.size()

        } catch (Exception ex) {
            println "Exception while writing file ${fname}, message: ${ex.message}"
        }



// Generate fat base class for services to use that provides direct access to Daos.

        decls = buckets.collect {

            def camelName = it.daoName
            camelName = camelName.substring(0, 1).toLowerCase() + camelName.substring(1);

            """
    @Inject
    public ${it.fullDaoName} ${camelName};


"""
        }.join('\n')

        fname = "${outdir}/AllJooqDaos.java"
        println "Generating file: ${fname}"

        def AllJooqDaos = """package ${parentPackage};

import org.jooq.impl.DefaultDSLContext;

import javax.inject.Inject;

${header}
public class AllJooqDaos {

    @Inject
    public DefaultDSLContext dsl;

    ${decls}

}
"""
        try {
            new File(fname).text = AllJooqDaos
            def file = new File(fname)
            println "Full path: " + file.absolutePath + " size: " + file.size()
        } catch (Exception ex) {
            println "Exception while writing file ${fname}, message: ${ex.message}"
        }

    }

}
