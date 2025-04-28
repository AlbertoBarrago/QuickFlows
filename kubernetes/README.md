# Kubernetes Examples

## What is Kubernetes?

Kubernetes (K8s) is an open-source container orchestration platform designed to automate the deployment, scaling, and management of containerized applications. Originally developed by Google and now maintained by the Cloud Native Computing Foundation (CNCF), Kubernetes provides a framework for running distributed systems resiliently, handling failover, scaling, and load balancing automatically.

## Why Kubernetes Exists

As containerization gained popularity with Docker, organizations needed a way to manage containers at scale. Running a few containers manually is straightforward, but managing hundreds or thousands of containers across multiple hosts requires automation. Kubernetes was created to address this challenge, providing a unified API to deploy and manage containerized workloads regardless of the underlying infrastructure.

## Problems Kubernetes Solves

- **Container Orchestration**: Automates the deployment and scaling of containerized applications
- **Service Discovery and Load Balancing**: Automatically distributes network traffic and exposes services
- **Self-healing**: Restarts failed containers, replaces and reschedules containers when nodes die
- **Automated Rollouts and Rollbacks**: Changes to application or configuration can be rolled out progressively and rolled back if something goes wrong
- **Horizontal Scaling**: Applications can be scaled manually or automatically based on CPU or custom metrics
- **Storage Orchestration**: Automatically mounts storage systems of your choice
- **Secret and Configuration Management**: Manages sensitive information and application configuration without rebuilding container images

## How to Use Kubernetes

### Basic Kubernetes Commands

```bash
# View all resources in the cluster
kubectl get all

# Deploy an application from a YAML file
kubectl apply -f deployment.yaml

# Scale a deployment
kubectl scale deployment/app-name --replicas=5

# View logs for a pod
kubectl logs pod-name

# Execute a command in a container
kubectl exec -it pod-name -- /bin/bash

# Delete a resource
kubectl delete -f deployment.yaml
```

### Kubernetes Architecture

Kubernetes follows a master-worker architecture:

- **Control Plane (Master)**: Manages the cluster with components like API Server, Scheduler, Controller Manager, and etcd
- **Nodes (Workers)**: Run the containerized applications with components like kubelet, kube-proxy, and container runtime

### Key Kubernetes Concepts

- **Pods**: The smallest deployable units that can be created and managed
- **Services**: An abstract way to expose an application running on a set of Pods
- **Deployments**: Declarative updates for Pods and ReplicaSets
- **ConfigMaps and Secrets**: Ways to separate configuration from application code
- **Namespaces**: Virtual clusters within a physical cluster

## Contents

- `deployments/`: Kubernetes deployment manifests for various application types
- `services/`: Service manifests for exposing applications within and outside the cluster
- `ingress/`: Ingress controller configurations for routing external traffic

## Usage

Each subdirectory contains detailed examples with comments explaining the purpose of each configuration option and how to customize it for your specific needs.

## License

This project is licensed under the MIT License - see the LICENSE file in the root directory for details.