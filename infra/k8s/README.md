# Kubernetes Manifests

This directory will contain Kubernetes deployment manifests when transitioning from Docker Compose to Kubernetes.

## Planned Structure

```
k8s/
├── base/                 # Base manifests (Kustomize)
│   ├── deployments/
│   ├── services/
│   ├── configmaps/
│   └── secrets/
├── overlays/
│   ├── dev/              # Development environment
│   ├── staging/          # Staging environment
│   └── prod/             # Production environment
└── helm/                 # Helm charts (optional)
```

## Note

Since Kubernetes provides native service discovery via DNS, Eureka/Service Registry is **not required** for this project.
