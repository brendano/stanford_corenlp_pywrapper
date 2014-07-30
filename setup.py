from setuptools import setup, find_packages  # Always prefer setuptools over distutils

setup(
    name='stanford_corepywrapper',

    version='0.0.1a',

    description='A Python wrapper for CoreNLP.',

    packages=['stanford_corepywrapper'],
    package_dir={'stanford_corepywrapper': 'stanford_corepywrapper'},
    package_data={
        'stanford_corepywrapper': ['javasrc/corenlp/*', 'lib/*',
                                   'javasrc/util/Arr.java',
                                   'javasrc/util/BasicFileIO.java',
                                   'javasrc/util/JsonUtil.java',
                                   'javasrc/util/U.java',
                                   'javasrc/util/misc/*'],
    }
)
