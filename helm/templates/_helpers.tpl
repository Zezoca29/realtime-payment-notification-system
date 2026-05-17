{{/*
Expand the name of the chart.
*/}}
{{- define "payment-system.name" -}}
{{- .Chart.Name | trunc 63 | trimSuffix "-" }}
{{- end }}

{{/*
Common labels applied to every resource.
*/}}
{{- define "payment-system.labels" -}}
helm.sh/chart: {{ .Chart.Name }}-{{ .Chart.Version }}
app.kubernetes.io/managed-by: {{ .Release.Service }}
app.kubernetes.io/instance: {{ .Release.Name }}
{{- end }}

{{/*
Selector labels for a given component.
Usage: {{ include "payment-system.selectorLabels" (dict "component" "payment-producer") }}
*/}}
{{- define "payment-system.selectorLabels" -}}
app.kubernetes.io/name: {{ .component }}
app.kubernetes.io/instance: {{ .Release.Name | default "release" }}
{{- end }}

{{/*
Full image reference: registry/image:tag
*/}}
{{- define "payment-system.image" -}}
{{- if .global.imageRegistry -}}
{{ .global.imageRegistry }}/{{ .image }}:{{ .global.imageTag }}
{{- else -}}
{{ .image }}:{{ .global.imageTag }}
{{- end }}
{{- end }}
