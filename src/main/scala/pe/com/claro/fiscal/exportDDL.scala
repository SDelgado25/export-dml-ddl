package pe.com.claro.fiscal

import org.apache.spark.sql.SparkSession
import org.apache.spark.sql.functions.{col, upper}
import pe.com.claro.util.UtilExport

object exportDDL {

  def main(args: Array[String]): Unit = {
    val env = args(0)
    val capa = args(1)
    val comando = args(2)

    val spark: SparkSession = SparkSession.builder()
      .appName(s"cmp-common-export-ddl-dml-scala")
      .config("spark.sql.hive.convertMetastoreOrc", "true")
      .config("spark.sql.orc.enabled", "true")
      .config("spark.sql.orc.filterPushdown", "true")
      .config("spark.sql.orc.splits.include.file.footer", "true")
      .config("spark.sql.hive.metastorePartitionPruning", "true")
      .config("spark.sql.orc.impl", "native")
      .config("spark.sql.orc.enableVectorizedReader", "true")
      .config("spark.hadoop.parquet.enable.summary-metadata", "false")
      .config("spark.sql.parquet.mergeSchema", "false")
      .config("spark.sql.parquet.filterPushdown", "true")
      .config("hive.exec.dynamic.partition", "true")
      .config("hive.exec.dynamic.partition.mode", "nonstrict")
      .enableHiveSupport()
      .getOrCreate()

    val ruta_temp=s"/claro/cfiscal/${env}/global/temp/export_temp"
    val ruta_destino=s"/claro/cfiscal/${env}/global/temp/export_target/ddl"
    /* Obtiene tablas para capa solicitada*/
    var dfBD = spark.sql(s"show databases")
    dfBD=dfBD.filter(upper(col("databaseName")).startsWith(env.toUpperCase()) &&
      upper(col("databaseName")).contains(s"cfiscal_${capa}".toUpperCase())
    )

    /* Obtiene listado de BD*/
    val listDB = dfBD.collect().map(value => value(0).toString)
    listDB.foreach(db =>
      try {
        val dfNombreTablas = spark.sql(s"show tables from ${db}")
        val listTablas = dfNombreTablas.collect().map(value => value(1).toString)
        if(listTablas.size>0)
        {
          listTablas.foreach(tabla =>
            try {
              val dfDescribe=spark.sql(s"${comando} ${db}.${tabla}")
              val  nombre_archivo_destino=s"${comando}_${db}_${tabla}"
              println(nombre_archivo_destino)
              UtilExport.exportCSV(spark,dfDescribe,ruta_temp,
                s"${ruta_destino}/${capa}/${comando}",
                nombre_archivo_destino)
            }
          )
        }
      }
    )

  }



}
