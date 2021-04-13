const snapshotControl = () => {
    const saveCanvas = (blob) => {
        let reader = new FileReader()
        reader.readAsDataURL(blob)

        reader.onload = function () {
            snapshot.data = reader.result

            const done = () => {
                $('#saveSnapshotModal').modal('close')
                M.toast({html: `Snapshot created!`})
            }
            const fail = (resp) => {
                M.toast({html: `Error ${resp.status}: ${resp.statusText}`})
            }

            $.post('/snapshots/save', JSON.stringify(snapshot)).done(done).fail(fail)
        }
    }

    const save = () => {
        const canvas = document.getElementById('wb-canvas')
        canvas.toBlob(saveCanvas, 'image/jpeg')
    }

    const restore = () => {
        console.log('restore')
    }

    const list = () => {

    }

    const snapshot = {
        name: '',
        user: '',
        data: ''
    }

    return {
        save,
        restore,
        list,
        snapshot
    }
}
