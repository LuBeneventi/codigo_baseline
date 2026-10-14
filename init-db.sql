-- Script de inicialización para crear las bases de datos de Catálogo y Órdenes
SELECT 'CREATE DATABASE pedidos360_catalog'
WHERE NOT EXISTS (SELECT FROM pg_database WHERE datname = 'pedidos360_catalog')\gexec

SELECT 'CREATE DATABASE pedidos360_orders'
WHERE NOT EXISTS (SELECT FROM pg_database WHERE datname = 'pedidos360_orders')\gexec

SELECT 'CREATE DATABASE pedidos360_auth'
WHERE NOT EXISTS (SELECT FROM pg_database WHERE datname = 'pedidos360_auth')\gexec

