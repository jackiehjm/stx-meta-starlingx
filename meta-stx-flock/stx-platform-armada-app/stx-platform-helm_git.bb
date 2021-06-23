SUMMARY = "StarlingX K8S application: Platform Integration"
DESCRIPTION = "StarlingX K8S application: Platform Integration"

LICENSE = "Apache-2.0"
LIC_FILES_CHKSUM = "file://${COREBASE}/meta/files/common-licenses/Apache-2.0;md5=89aea4e17d99a7cacdbeed46a0096b10"

DEPENDS += " \
    helm-native \
    openstack-helm-infra \
    python-k8sapp-platform \
    python-k8sapp-platform-wheels \
"

PROTOCOL = "https"
BRANCH = "r/stx.5.0"
SRCREV = "42b97c591a38167623f6dccb35f3b3ff67fd78db"

PV = "1.0.0+git${SRCPV}"

inherit allarch
inherit stx-chartmuseum
inherit stx-metadata

STX_REPO = "helm-charts"
STX_SUBPATH = "node-feature-discovery/node-feature-discovery/helm-charts"


SRC_URI = " \
    git://opendev.org/starlingx/platform-armada-app.git;protocol=${PROTOCOL};branch=${BRANCH} \
"

S = "${WORKDIR}/git/stx-platform-helm/stx-platform-helm"


helm_repo = "stx-platform"
toolkit_version = "0.1.0"
helm_folder = "${RECIPE_SYSROOT}${nonarch_libdir}/helm"

app_name = "platform-integ-apps"
app_staging = "${B}/staging"
app_tarball = "${app_name}-${PV}.tgz"
app_folder = "/usr/local/share/applications/helm"

do_configure[noexec] = "1"

do_compile () {
	# Stage helm-toolkit in the local repo
	cp ${helm_folder}/helm-toolkit-${toolkit_version}.tgz ${S}/helm-charts/

	# Host a server for the charts
	chartmuseum --debug --port=${CHARTMUSEUM_PORT} --context-path='/charts' --storage="local" --storage-local-rootdir="./helm-charts" &
	sleep 2
	helm repo add local http://localhost:${CHARTMUSEUM_PORT}/charts

	# Make the charts. These produce a tgz file
	cp -rf ${STX_METADATA_PATH}/node-feature-discovery/ ${S}/helm-charts/
	cd ${S}/helm-charts
	make rbd-provisioner
	make ceph-pools-audit
	make cephfs-provisioner
	make node-feature-discovery
	cd -

	# Terminate helm server (the last backgrounded task)
	kill $!

	# Create a chart tarball compliant with sysinv kube-app.py
	# Setup staging
	mkdir -p ${app_staging}
	cp ${S}/files/metadata.yaml ${app_staging}
	cp ${S}/manifests/manifest.yaml ${app_staging}

	mkdir -p ${app_staging}/charts
	cp ${S}/helm-charts/*.tgz ${app_staging}/charts
	cd ${app_staging}

	# Populate metadata
	sed -i 's/@APP_NAME@/${app_name}/g' ${app_staging}/metadata.yaml
	sed -i 's/@APP_VERSION@/${PV}/g' ${app_staging}/metadata.yaml
	sed -i 's/@HELM_REPO@/${helm_repo}/g' ${app_staging}/metadata.yaml

	# Copy the plugins: installed in the buildroot
	mkdir -p ${app_staging}/plugins
	cp ${RECIPE_SYSROOT}/plugins/*.whl ${app_staging}/plugins

	# package it up
	find . -type f ! -name '*.md5' -print0 | xargs -0 md5sum > checksum.md5
	tar -zcf ${B}/${app_tarball} -C ${app_staging}/ .

	# Cleanup staging
	rm -fr ${app_staging}
}

do_install () {
	install -d -m 755 ${D}/${app_folder}
	install -p -D -m 755 ${B}/${app_tarball} ${D}/${app_folder}
	install -d -m 755 ${D}/opt/extracharts
	install -p -D -m 755 ${S}/helm-charts/node-feature-discovery-*.tgz ${D}/opt/extracharts
}

FILES_${PN} = " \
    /opt/extracharts \
    ${app_folder} \
"

RDEPENDS_${PN} = " \
    helm \
    openstack-helm-infra \
"